"""第3轮：A1(API) + B1(巡检连跑两次) + C(overview 口径)

仅调用：GET score-anomalies / GET data-audit/issues / POST DATA_AUDIT_TASK/run ×2（team-lead 已授权）
"""
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8080"
MYSQL = r"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"
OUT = []


def call(method, path, token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode() if body else None
    h = {"Accept": "application/json"}
    if body:
        h["Content-Type"] = "application/json; charset=utf-8"
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:  # noqa: BLE001
            return e.code, {"raw": raw}


def mysql_q(sql):
    p = subprocess.run([MYSQL, "-h100.83.74.68", "-P3306", "-uroot", "-p123456",
                        "--default-character-set=utf8mb4", "-N", "-B", "-e",
                        "USE edu_course_selection; " + sql],
                       capture_output=True, text=True, encoding="utf-8")
    return p.stdout.strip()


def login():
    st, js = call("POST", "/api/v1/auth/login",
                  body={"username": "admin", "password": "123456", "loginType": "ADMIN"})
    return (js.get("data") or {}).get("token")


def main():
    admin = login()
    print("admin token:", bool(admin))
    if not admin:
        return

    # ---------- A1 ----------
    print("\n===== A1: sem5 成绩异常 =====")
    st, js = call("GET", "/api/v1/admin/analytics/score-anomalies?semesterId=5&page=1&pageSize=200", admin)
    d = js.get("data") or {}
    recs = d.get("records") or []
    from collections import Counter
    types = Counter(t for r in recs for t in r["anomalyTypes"])
    ids = {r["teachingClassId"] for r in recs}
    print(f"  total={d.get('total')} len={len(recs)} types={dict(types)}")
    print(f"  1014 in list: {1014 in ids};  739: {739 in ids};  759: {759 in ids}")
    print(f"  scoredCount=0 假阳性: {sum(1 for r in recs if r['scoredCount']==0)}")
    OUT.append({"item": "A1", "total": d.get("total"), "types": dict(types),
                "has1014": 1014 in ids, "has739": 739 in ids, "has759": 759 in ids,
                "zero_scored": sum(1 for r in recs if r["scoredCount"] == 0)})

    # ---------- B1: 巡检连跑两次 ----------
    print("\n===== B1: 巡检连跑两次 =====")
    runs = []
    for i in (1, 2):
        before = mysql_q("SELECT COUNT(*), GROUP_CONCAT(DISTINCT stat_date) FROM data_audit_issue;")
        t0 = time.time()
        st, js = call("POST", "/api/v1/admin/schedule-tasks/DATA_AUDIT_TASK/run", admin)
        dur = round(time.time() - t0, 1)
        rec = {"round": i, "http": st, "code": js.get("code"), "elapsed_s": dur,
               "data": js.get("data"), "before": before}
        after = mysql_q("SELECT COUNT(*), GROUP_CONCAT(DISTINCT stat_date) FROM data_audit_issue;")
        rec["after"] = after
        runs.append(rec)
        print(f"  第{i}次 HTTP={st} code={js.get('code')} 耗时={dur}s")
        print(f"       返回={json.dumps(js.get('data'), ensure_ascii=False)}")
        print(f"       落库 前=[{before}] 后=[{after}]")
        time.sleep(2)
    OUT.append({"item": "B1", "runs": runs})

    # ---------- B1 后置核对 ----------
    print("\n  --- B1 后置核对 ---")
    print("  分组:", mysql_q("SELECT rule_code, biz_type, COUNT(*) FROM data_audit_issue "
                            "GROUP BY rule_code, biz_type ORDER BY rule_code, biz_type;").replace("\t", "="))
    print("  总数/日期:", mysql_q("SELECT COUNT(*), GROUP_CONCAT(DISTINCT stat_date) FROM data_audit_issue;"))
    st, js = call("GET", "/api/v1/admin/data-audit/issues?page=1&pageSize=200", admin)
    print("  API total:", (js.get("data") or {}).get("total"))

    # ---------- 巡检日志 ----------
    print("\n  --- schedule_task_log 最近记录 ---")
    print(mysql_q("SELECT id, task_code, trigger_type, status, scan_count, success_count, fail_count "
                  "FROM schedule_task_log WHERE task_code='DATA_AUDIT_TASK' ORDER BY id DESC LIMIT 4;"
                  ).replace("\t", " | "))

    with open("R3_api_result.json", "w", encoding="utf-8") as f:
        json.dump(OUT, f, ensure_ascii=False, indent=2, default=str)
    print("\n已写入 R3_api_result.json")


if __name__ == "__main__":
    main()
