"""QA 接口实测脚本（只读，仅 GET + 登录）

用法：
    python api_probe.py [base_url]
输出：
    控制台摘要 + api_probe_result.json
"""
import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080"
OUT = []
OK_CODE = 200


def call(method, path, token=None, body=None, timeout=180):
    url = BASE + path
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:  # noqa: BLE001
        return -1, f"ERROR: {e}"


def login(username, password, login_type):
    st, raw = call("POST", "/api/v1/auth/login",
                   body={"username": username, "password": password, "loginType": login_type})
    try:
        js = json.loads(raw)
    except Exception:  # noqa: BLE001
        return None, st, raw
    return (js.get("data") or {}).get("token"), st, raw


def brief(raw, limit=1200):
    return raw if len(raw) <= limit else raw[:limit] + "...<truncated>"


def summarize(d):
    if d is None:
        return "data=null"
    if isinstance(d, list):
        return f"list len={len(d)}"
    if isinstance(d, dict):
        for k in ("records", "list", "items", "content", "rows"):
            if isinstance(d.get(k), list):
                return f"{k} len={len(d[k])}; total={d.get('total')}"
        return "obj keys=" + ",".join(list(d.keys())[:14])
    return type(d).__name__


def samples_of(d, show=2):
    if isinstance(d, list):
        return d[:show]
    if isinstance(d, dict):
        for k in ("records", "list", "items"):
            if isinstance(d.get(k), list):
                return d[k][:show]
        return {k: d[k] for k in list(d.keys())[:10]}
    return d


def probe(name, path, token, expect=OK_CODE, show=2):
    st, raw = call("GET", path, token)
    entry = {"name": name, "path": path, "http": st}
    try:
        js = json.loads(raw)
        entry["code"] = js.get("code")
        entry["message"] = js.get("message")
        entry["count"] = summarize(js.get("data"))
        entry["sample"] = json.loads(json.dumps(samples_of(js.get("data"), show),
                                                 ensure_ascii=False, default=str))
    except Exception:  # noqa: BLE001
        entry["code"] = "NON_JSON"
    entry["ok"] = st == 200 and entry.get("code") == expect
    entry["raw_head"] = brief(raw)
    OUT.append(entry)
    print(f"[{'PASS' if entry['ok'] else 'FAIL'}] {name}: HTTP={st} code={entry.get('code')} {entry.get('count')}")
    if not entry["ok"]:
        print("        " + brief(raw, 300))
    return entry


def main():
    admin, st, raw = login("admin", "123456", "ADMIN")
    print("admin login:", st, "OK" if admin else brief(raw, 200))
    stu, st, raw = login("20260001", "123456", "STUDENT")
    print("student 20260001 login:", st, "OK" if stu else brief(raw, 200))
    tea, st, raw = login("T10005", "123456", "TEACHER")
    print("teacher T10005 login:", st, "OK" if tea else brief(raw, 200))
    tea1, st, raw = login("T10001", "123456", "TEACHER")
    print("teacher T10001 login:", st, "OK" if tea1 else brief(raw, 200))
    for nm, tk in (("admin", admin), ("student-20260001", stu), ("teacher-T10005", tea), ("teacher-T10001", tea1)):
        OUT.append({"name": f"login-{nm}", "http": 200 if tk else 0, "token_ok": bool(tk)})
    if not admin:
        dump()
        return

    probe("1 看板概览", "/api/v1/admin/dashboard/overview?semesterId=5", admin)
    probe("2 毕业风险名单", "/api/v1/admin/analytics/graduation-risks?semesterId=5&page=1&pageSize=20", admin)
    probe("3 选课热度预测", "/api/v1/admin/analytics/enrollment-forecast?semesterId=5", admin)
    probe("4 成绩异常班级", "/api/v1/admin/analytics/score-anomalies?semesterId=5&page=1&pageSize=20", admin)
    probe("5a 教室推荐(无参)", "/api/v1/admin/analytics/classroom-recommendations", admin)
    probe("5b 教室推荐(按教学班 738)",
          "/api/v1/admin/analytics/classroom-recommendations?teachingClassId=738&topN=5", admin)
    probe("5c 教室推荐(按时段)",
          "/api/v1/admin/analytics/classroom-recommendations?weekday=3&startSection=5&endSection=6"
          "&startWeek=1&endWeek=16&topN=5", admin)
    probe("6 教室利用率", "/api/v1/admin/statistics/classroom-usage?semesterId=5&page=1&pageSize=100", admin)
    probe("7 教师工作量", "/api/v1/admin/statistics/teacher-workload?semesterId=5&page=1&pageSize=20", admin)
    probe("8 选课统计", "/api/v1/admin/statistics/course-selection?semesterId=5&page=1&pageSize=20", admin)
    probe("9 数据巡检问题", "/api/v1/admin/data-audit/issues?page=1&pageSize=20", admin)
    probe("9b 巡检-教室冲突", "/api/v1/admin/data-audit/issues?ruleCode=SCHEDULE_CONFLICT&bizType=CLASSROOM&page=1&pageSize=100", admin)
    probe("9c 巡检-教师冲突", "/api/v1/admin/data-audit/issues?ruleCode=SCHEDULE_CONFLICT&bizType=TEACHER&page=1&pageSize=100", admin)
    probe("10 学分预警", "/api/v1/admin/warnings/credit?semesterId=5&page=1&pageSize=20", admin)

    if stu:
        probe("11a 学生毕业风险", "/api/v1/student/graduation-risk", stu)
        probe("11b 学生学分进度", "/api/v1/student/credit-progress", stu)
        probe("11c 学生学分进度(sem9 历史)", "/api/v1/student/credit-progress?semesterId=9", stu)

    if tea:
        probe("12a 教师工作量", "/api/v1/teacher/workload", tea)
        probe("12b 教师成绩预警", "/api/v1/teacher/score-warnings", tea)
        probe("12c 教师教学班列表", "/api/v1/teacher/teaching-classes?page=1&pageSize=30", tea)
        probe("12d 教学班成绩统计(738)", "/api/v1/teacher/teaching-classes/738/score-statistics", tea)
        probe("12e 教学班成绩统计(759)", "/api/v1/teacher/teaching-classes/759/score-statistics", tea)
    if tea1:
        probe("13a 教师T10001工作量", "/api/v1/teacher/workload", tea1)
        probe("13b 教师T10001教学班", "/api/v1/teacher/teaching-classes?page=1&pageSize=30", tea1)

    dump()


def dump():
    with open("api_probe_result.json", "w", encoding="utf-8") as f:
        json.dump(OUT, f, ensure_ascii=False, indent=2, default=str)
    print("\n结果已写入 api_probe_result.json")


if __name__ == "__main__":
    main()
