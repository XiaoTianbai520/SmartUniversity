#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
SmartUniversity 选课链路三档并发压测脚本（V1.2 W2-3）

覆盖四个场景：
  1. waitlist（候补递补超卖窗口）：教学班已满，多学生并发选 -> 全部进入候补；
     用于观察候补递补路径在并发下是否产生超卖（需结合递补定时任务/手动递补触发，
     本脚本只负责制造并发候补写入，超卖结论见 docs/V1.2_压测报告.md）。
  2. main（主链路并发抢最后 1 名额）：教学班仅剩 1 名额，N 个不同学生并发抢 -> 仅 1 人成功。
  3. duplicate（重复选课）：同一学生并发重复选同一教学班 -> 仅成功 1 条。
  4. degrade（Redis 不可用降级路径）：Redis 宕机后回落 DB 判定，选课不应抛 5xx（需运维停 Redis 后运行）。

用法示例：
  # 主链路：教学班 10 仅剩 1 名额，100 并发抢（学生 20260001~20260100）
  python selection_stress.py --mode main --teaching-class-id 10 --threads 100 \
      --student-prefix 20260 --student-start 1 --student-count 100 --student-pass 123456

  # 重复选课：学生 20260001 并发 50 次选教学班 11
  python selection_stress.py --mode duplicate --teaching-class-id 11 --threads 50 \
      --student 20260001 --student-pass 123456

  # 候补：教学班已满（容量 60 已选 60），500 并发选
  python selection_stress.py --mode waitlist --teaching-class-id 12 --threads 500 \
      --student-prefix 20260 --student-start 1 --student-count 500 --student-pass 123456

  # 降级：先停 Redis，再并发选课，验证无 5xx
  python selection_stress.py --mode degrade --teaching-class-id 13 --threads 100 \
      --student-prefix 20260 --student-start 1 --student-count 100 --student-pass 123456

说明：
  - 仅依赖 Python 标准库（urllib / threading / argparse / json / time），无需 pip 安装。
  - 目标学期需处于选课窗口内（IN_PROGRESS 批次）。若当前学期无进行中批次，可加
    --flip-semester-id 将另一个学期临时置 PLANNED（脚本 finally 中恢复为 ACTIVE）。
  - 六项指标的「数据库连接数」「Redis 请求情况」为服务端指标，脚本无法直接读取，
    输出中明确标注 N/A 并给出服务端采集方式（Actuator / SHOW STATUS / redis INFO）。
"""

import argparse
import json
import sys
import threading
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"


def call(method, path, token=None, body=None, timeout=25):
    """发起一次 HTTP 调用，返回 (http_status, payload_dict, latency_ms)。"""
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    start = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            payload = resp.read().decode("utf-8")
            try:
                return resp.status, json.loads(payload), int((time.time() - start) * 1000)
            except Exception:
                return resp.status, {"raw": payload[:200]}, int((time.time() - start) * 1000)
    except urllib.error.HTTPError as ex:
        payload = ex.read().decode("utf-8", errors="replace")
        try:
            return ex.code, json.loads(payload), int((time.time() - start) * 1000)
        except Exception:
            return ex.code, {"raw": payload[:200]}, int((time.time() - start) * 1000)
    except Exception as ex:
        return 0, {"error": str(ex)}, int((time.time() - start) * 1000)


def login(username, password, login_type, timeout=25):
    """登录并取回 token，失败返回 None。"""
    status, resp, _ = call("POST", "/auth/login",
                           body={"username": username, "password": password, "loginType": login_type},
                           timeout=timeout)
    if status == 200 and (resp or {}).get("code") == 200:
        return (resp or {}).get("data", {}).get("token")
    return None


def student_username(prefix, index):
    return "%s%05d" % (prefix, index)


def worker_select(records, lock, idx, token, teaching_class_id, result_holder):
    """单个并发线程：持 token 选一次教学班，记录结果。"""
    status, resp, latency = call("POST", "/student/selections",
                                token=token, body={"teachingClassId": teaching_class_id})
    code = (resp or {}).get("code")
    ok = (status == 200 and code == 200)
    with lock:
        records.append({
            "thread": idx,
            "http_status": status,
            "code": code,
            "latency_ms": latency,
            "success": ok,
            "message": (resp or {}).get("message"),
        })
        if ok:
            result_holder["success"] += 1


def run_stress(args):
    summary = {
        "mode": args.mode,
        "base_url": BASE,
        "teaching_class_id": args.teaching_class_id,
        "threads": args.threads,
        "capacity": args.capacity,
        "started_at": time.strftime("%Y-%m-%d %H:%M:%S"),
        "total": 0,
        "success": 0,
        "fail": 0,
        "http_5xx": 0,
        "latencies_ms": [],
        "code_distribution": {},
        "oversell": None,
        "duplicate": None,
        "db_connection_count": "N/A（服务端指标：Actuator / SHOW STATUS LIKE 'Threads_connected'）",
        "redis_requests": "N/A（服务端指标：redis-cli INFO stats / INFO commandstats）",
    }

    admin_token = None
    flipped = False
    try:
        # 管理员登录（用于可选学期翻转与结束后恢复）
        admin_token = login(args.admin_user, args.admin_pass, "ADMIN")
        if args.flip_semester_id and admin_token:
            st, r = call("PATCH", "/admin/semesters/%d/status" % args.flip_semester_id,
                         token=admin_token, body={"status": "PLANNED"})
            if st == 200 and (r or {}).get("code") == 200:
                flipped = True
                print("[INFO] 已将 semester %d 置 PLANNED，使目标学期成为当前学期" % args.flip_semester_id)

        records = []
        result_holder = {"success": 0}
        lock = threading.Lock()
        threads = []

        if args.mode in ("main", "waitlist"):
            # 每个线程登录一个独立学生再选课
            for i in range(args.threads):
                idx = i + 1
                username = student_username(args.student_prefix, args.student_start + i)
                token = login(username, args.student_pass, "STUDENT")
                if token is None:
                    with lock:
                        records.append({"thread": idx, "http_status": 0, "code": None,
                                        "latency_ms": 0, "success": False, "message": "login-failed:%s" % username})
                    continue
                t = threading.Thread(target=worker_select,
                                    args=(records, lock, idx, token, args.teaching_class_id, result_holder))
                threads.append(t)
        elif args.mode == "duplicate":
            # 同一学生并发重复选同一教学班
            token = login(args.student, args.student_pass, "STUDENT")
            if token is None:
                print("[FAIL] 学生 %s 登录失败" % args.student)
                return summary
            for i in range(args.threads):
                t = threading.Thread(target=worker_select,
                                    args=(records, lock, i + 1, token, args.teaching_class_id, result_holder))
                threads.append(t)
        elif args.mode == "degrade":
            # 多学生并发选课，观察 Redis 宕机降级后是否抛 5xx（需运维先停 Redis）
            for i in range(args.threads):
                idx = i + 1
                username = student_username(args.student_prefix, args.student_start + i)
                token = login(username, args.student_pass, "STUDENT")
                if token is None:
                    with lock:
                        records.append({"thread": idx, "http_status": 0, "code": None,
                                        "latency_ms": 0, "success": False, "message": "login-failed:%s" % username})
                    continue
                t = threading.Thread(target=worker_select,
                                    args=(records, lock, idx, token, args.teaching_class_id, result_holder))
                threads.append(t)
        else:
            print("[FAIL] 未知模式：%s" % args.mode)
            return summary

        start = time.time()
        for each in threads:
            each.start()
        for each in threads:
            each.join()
        wall_ms = int((time.time() - start) * 1000)

        # 汇总
        total = len(records)
        success = sum(1 for each in records if each["success"])
        http_5xx = sum(1 for each in records if each["http_status"] >= 500)
        latencies = [each["latency_ms"] for each in records if each["latency_ms"] > 0]
        code_dist = {}
        for each in records:
            key = str(each["code"])
            code_dist[key] = code_dist.get(key, 0) + 1

        avg_latency = (sum(latencies) / len(latencies)) if latencies else 0
        max_latency = max(latencies) if latencies else 0
        min_latency = min(latencies) if latencies else 0

        # 是否超卖：以「API 层成功数」对照容量（权威口径需以 DB COUNT 复核，见报告）
        if args.mode in ("main", "waitlist") and args.capacity is not None:
            summary["oversell"] = (success > args.capacity)
        # 是否重复选课：同一学生并发成功多于 1 条即为重复选课未被拦截
        if args.mode == "duplicate":
            summary["duplicate"] = (success > 1)

        summary.update({
            "total": total,
            "success": success,
            "fail": total - success,
            "http_5xx": http_5xx,
            "wall_time_ms": wall_ms,
            "success_rate": round(success / total, 4) if total else 0,
            "avg_latency_ms": round(avg_latency, 2),
            "min_latency_ms": min_latency,
            "max_latency_ms": max_latency,
            "code_distribution": code_dist,
            "latencies_ms": latencies,
        })

        # 控制台可读输出
        print("\n================ 压测原始数据 ================")
        print(json.dumps({k: v for k, v in summary.items() if k != "latencies_ms"},
                         ensure_ascii=False, indent=2))
        print("平均响应时间(ms)：%.2f  最小：%d  最大：%d  墙钟耗时：%dms"
              % (avg_latency, min_latency, max_latency, wall_ms))
        print("成功率：%.2f%%" % (summary["success_rate"] * 100))
        print("HTTP 5xx 数：%d" % http_5xx)
        print("是否超卖（API 层成功数 > 容量）：%s" % summary["oversell"])
        print("是否重复选课（同学生成功 > 1）：%s" % summary["duplicate"])
        print("数据库连接数：%s" % summary["db_connection_count"])
        print("Redis 请求情况：%s" % summary["redis_requests"])
        print("=============================================")
        return summary
    finally:
        if flipped and admin_token:
            st, r = call("PATCH", "/admin/semesters/%d/status" % args.flip_semester_id,
                         token=admin_token, body={"status": "ACTIVE"})
            print("[INFO] 已恢复 semester %d 为 ACTIVE（HTTP %s，code=%s）"
                  % (args.flip_semester_id, st, (r or {}).get("code")))


def parse_args():
    p = argparse.ArgumentParser(description="SmartUniversity 选课链路并发压测")
    p.add_argument("--mode", required=True, choices=["main", "waitlist", "duplicate", "degrade"],
                   help="压测场景：main=主链路抢最后1名额 / waitlist=候补超卖窗口 / "
                        "duplicate=重复选课 / degrade=Redis降级路径")
    p.add_argument("--base-url", default=BASE, help="目标服务基址，默认 %s" % BASE)
    p.add_argument("--teaching-class-id", type=int, required=True, help="目标教学班 ID")
    p.add_argument("--threads", type=int, default=100, help="并发线程数（100/500/1000）")
    p.add_argument("--capacity", type=int, default=None,
                   help="目标教学班容量（用于超卖判定；不传则不判定超卖）")
    p.add_argument("--student", default=None, help="duplicate 模式：单个学生学号")
    p.add_argument("--student-prefix", default="20260", help="多学生模式：学号前缀")
    p.add_argument("--student-start", type=int, default=1, help="多学生模式：学号起始序号")
    p.add_argument("--student-count", type=int, default=None,
                   help="多学生模式：实际使用的学生数（默认=threads）")
    p.add_argument("--student-pass", default="123456", help="学生登录密码")
    p.add_argument("--admin-user", default="admin", help="管理员用户名")
    p.add_argument("--admin-pass", default="123456", help="管理员密码")
    p.add_argument("--flip-semester-id", type=int, default=None,
                   help="可选：将其临时置 PLANNED 以让目标学期成为当前学期（脚本结束恢复）")
    p.add_argument("--out", default=None, help="可选：将汇总 JSON 写入该文件")
    return p.parse_args()


def main():
    args = parse_args()
    global BASE
    BASE = args.base_url.rstrip("/")
    if args.student_count is None:
        args.student_count = args.threads
    # 多学生模式实际线程数受 student-count 限制
    if args.mode in ("main", "waitlist", "degrade") and args.student_count < args.threads:
        args.threads = args.student_count
    summary = run_stress(args)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            json.dump(summary, f, ensure_ascii=False, indent=2)
        print("[INFO] 汇总已写入 %s" % args.out)
    return 0 if summary.get("total", 0) > 0 else 2


if __name__ == "__main__":
    sys.exit(main())
