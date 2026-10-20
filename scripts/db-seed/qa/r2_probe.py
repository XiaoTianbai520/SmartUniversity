"""QA-2 接口复验（只读，仅 GET + 登录，绝不调用 POST）"""
import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080"
OUT = []


def call(method, path, token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body else None
    h = {"Accept": "application/json"}
    if body:
        h["Content-Type"] = "application/json; charset=utf-8"
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=240) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:  # noqa: BLE001
            return e.code, {"raw": raw}
    except Exception as e:  # noqa: BLE001
        return -1, {"raw": str(e)}


def login(u, p, t):
    st, js = call("POST", "/api/v1/auth/login", body={"username": u, "password": p, "loginType": t})
    return (js.get("data") or {}).get("token")


def brief(o, n=700):
    s = json.dumps(o, ensure_ascii=False)
    return s if len(s) <= n else s[:n] + "..."


def rec(name, verdict, detail, sample=None):
    OUT.append({"name": name, "pass": verdict, "detail": detail, "sample": sample})
    print(f"[{'PASS' if verdict else 'FAIL'}] {name}: {detail}")


admin = login("admin", "123456", "ADMIN")
print("admin token:", bool(admin))

# ---------- 1. M-01 remainingSemesters ----------
print("\n===== 1. M-01 remainingSemesters =====")
EXPECT = {1: ("2022级", 1), 2: ("2023级", 2), 3: ("2024级", 4), 4: ("2025级", 6), 5: ("2026级", 8)}
for gid, (gname, exp) in EXPECT.items():
    st, js = call("GET", f"/api/v1/admin/analytics/graduation-risks?semesterId=5&gradeId={gid}&page=1&pageSize=1", admin)
    d = js.get("data") or {}
    rs = d.get("records") or []
    if not rs:
        rec(f"1 sem5 {gname}", False, "无记录")
        continue
    got = rs[0]["remainingSemesters"]
    rec(f"1 sem5 {gname} remainingSemesters", got == exp,
        f"期望 {exp} 实测 {got}（{rs[0]['studentNo']} risk={rs[0]['riskLevel']}）")

print("\n--- 1b 边界：semesterId=9 (2022-2023-1) ---")
for gid, gname in ((1, "2022级"), (5, "2026级")):
    st, js = call("GET", f"/api/v1/admin/analytics/graduation-risks?semesterId=9&gradeId={gid}&page=1&pageSize=1", admin)
    rs = (js.get("data") or {}).get("records") or []
    got = rs[0]["remainingSemesters"] if rs else None
    ok = (got == 8) if gid == 1 else (got is not None and got > 0)
    rec(f"1b sem9 {gname}", ok, f"remainingSemesters={got}（2022级期望 8；2026级只需 >0 且非 0/负）",
        rs[0] if rs else None)

# ---------- 2. M-02 成绩异常 ----------
print("\n===== 2. M-02 成绩异常 =====")
st, js = call("GET", "/api/v1/admin/analytics/score-anomalies?semesterId=5&page=1&pageSize=500", admin)
d = js.get("data") or {}
recs = d.get("records") or []
total = d.get("total")
ids = {r["teachingClassId"] for r in recs}
zero = [r for r in recs if r["scoredCount"] == 0]
from collections import Counter
types = Counter(t for r in recs for t in r["anomalyTypes"])
rec("2 sem5 异常 total", total == 23, f"期望 23 实测 {total}")
print("    异常类型分布:", dict(types))
rec("2b sem5 无 scoredCount=0 的假阳性", len(zero) == 0, f"scoredCount=0 条数={len(zero)}")
rec("2c 739 仍被检出", 739 in ids, "739 in records")
rec("2d 759 仍被检出", 759 in ids, "759 in records")
for tid in (739, 759):
    r = next((x for x in recs if x["teachingClassId"] == tid), None)
    if r:
        print(f"    {tid}: pass={r['passRate']} exc={r['excellentRate']} base={r['baselinePassRate']} types={r['anomalyTypes']}")

print("\n--- 2e semesterId=4 的 373（SCORE_MISSING 样本） ---")
st, js = call("GET", "/api/v1/admin/analytics/score-anomalies?semesterId=4&anomalyType=SCORE_MISSING&page=1&pageSize=500", admin)
d4 = js.get("data") or {}
r4 = d4.get("records") or []
ids4 = {r["teachingClassId"] for r in r4}
rec("2e sem4 SCORE_MISSING 命中 373", 373 in ids4, f"sem4 SCORE_MISSING total={d4.get('total')}，373 命中={373 in ids4}")
r373 = next((x for x in r4 if x["teachingClassId"] == 373), None)
if r373:
    print("    373:", brief({k: r373[k] for k in ('selectedCount', 'scoredCount', 'inputCompleteRate', 'anomalyTypes')}))

# ---------- 3. S-01 学分进度按学期演进 ----------
print("\n===== 3. S-01 学分进度 =====")
stu = login("20260001", "123456", "STUDENT")
if stu:
    st, js = call("GET", "/api/v1/student/credit-progress?semesterId=5", stu)
    d5 = js.get("data") or {}
    st, js9 = call("GET", "/api/v1/student/credit-progress?semesterId=9", stu)
    d9 = js9.get("data") or {}
    rec("3 同一学生 sem5 vs sem9 累计学分应递增",
        (d5.get("completedCredit") or 0) > (d9.get("completedCredit") or 0),
        f"sem5 completed={d5.get('completedCredit')} / sem9 completed={d9.get('completedCredit')}")

# ---------- 12/14 教师端 S-02 ----------
print("\n===== 14. 教师端 T10001 / T10002 =====")
for no, exp_cls in (("T10001", 2), ("T10002", 4)):
    tk = login(no, "123456", "TEACHER")
    if not tk:
        rec(f"14 {no} 登录", False, "登录失败")
        continue
    st, js = call("GET", "/api/v1/teacher/workload", tk)
    w = js.get("data") or {}
    cls = w.get("classes") or []
    rec(f"14 {no} workload.classCount", w.get("classCount") == exp_cls,
        f"期望 {exp_cls} 实测 {w.get('classCount')}（totalHours={w.get('totalHours')} 班级数组={len(cls)}）")
    st, js = call("GET", "/api/v1/teacher/teaching-classes", tk)
    lst = js.get("data") or []
    rec(f"14 {no} teaching-classes 非空", len(lst) > 0, f"len={len(lst)}")
    if lst:
        cid = lst[0].get("teachingClassId") or lst[0].get("id")
        st, js = call("GET", f"/api/v1/teacher/teaching-classes/{cid}/score-statistics", tk)
        rec(f"14 {no} 成绩统计可用", js.get("code") == 200, f"class={cid} code={js.get('code')}")
    st, js = call("GET", "/api/v1/teacher/score-warnings", tk)
    sw = js.get("data") or {}
    zero_sw = [r for r in (sw.get("records") or []) if r.get("scoredCount") == 0]
    rec(f"14 {no} 成绩预警无 scoredCount=0 假阳性", len(zero_sw) == 0,
        f"total={sw.get('total')}，scoredCount=0 条数={len(zero_sw)}")

# ---------- 11 毕业风险分布 + 10 成绩分布口径交叉 ----------
print("\n===== 11/12 分布核对（API 口径） =====")
for lv in ("HIGH", "MEDIUM", "LOW"):
    st, js = call("GET", f"/api/v1/admin/analytics/graduation-risks?semesterId=5&riskLevel={lv}&page=1&pageSize=1", admin)
    print(f"    graduation-risks {lv}: total={(js.get('data') or {}).get('total')}")
st, js = call("GET", "/api/v1/admin/dashboard/overview?semesterId=5", admin)
ov = js.get("data") or {}
print("    overview:", brief({k: ov.get(k) for k in
      ('semesterCode', 'studentCount', 'selectedStudentCount', 'courseCount', 'teachingClassCount',
       'teacherCount', 'selectionCount', 'waitlistCount', 'withdrawCount', 'fullClassRate',
       'classroomUsageRate', 'avgTeacherWorkload', 'avgCreditProgressRate')}))
OUT.append({"name": "overview", "detail": ov})

st, js = call("GET", "/api/v1/admin/data-audit/issues?page=1&pageSize=200", admin)
da = js.get("data") or {}
from collections import Counter
c = Counter((r["ruleCode"], r["bizType"]) for r in (da.get("records") or []))
print(f"    data-audit total={da.get('total')} 分组={dict(c)}")
rec("4 巡检 total=98", da.get("total") == 98, f"实测 {da.get('total')}")

with open("R2_api_result.json", "w", encoding="utf-8") as f:
    json.dump(OUT, f, ensure_ascii=False, indent=2, default=str)
print("\n已写入 R2_api_result.json")
