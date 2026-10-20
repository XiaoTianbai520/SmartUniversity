"""QA 深度探针：成绩异常假阳性占比、毕业风险剩余学期、看板口径交叉核对"""
import json
import urllib.request

BASE = "http://127.0.0.1:8080"


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body else None
    h = {"Accept": "application/json"}
    if body:
        h["Content-Type"] = "application/json"
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8", "replace"))


def login(u, p, t):
    _, js = call("POST", "/api/v1/auth/login", body={"username": u, "password": p, "loginType": t})
    return (js.get("data") or {}).get("token")


admin = login("admin", "123456", "ADMIN")
print("admin token ok:", bool(admin))

# 1) 成绩异常全量
st, js = call("GET", "/api/v1/admin/analytics/score-anomalies?semesterId=5&page=1&pageSize=500", admin)
recs = js["data"]["records"]
total = js["data"]["total"]
print(f"\n[成绩异常] total={total} 实际返回={len(recs)}")
zero = [r for r in recs if r["scoredCount"] == 0]
print(f"  其中 scoredCount=0 的记录数: {len(zero)}  占比 {len(zero)/max(len(recs),1)*100:.1f}%")
from collections import Counter
c = Counter()
for r in recs:
    for t in r["anomalyTypes"]:
        c[t] += 1
print("  异常类型分布:", dict(c))
print("  scoredCount=0 的 anomalyTypes 分布:", dict(Counter(t for r in zero for t in r["anomalyTypes"])))
print("  scoredCount=0 样例:", json.dumps(
    [{k: zero[0][k] for k in ('teachingClassId', 'classCode', 'selectedCount', 'scoredCount',
                              'passRate', 'baselinePassRate', 'deviation', 'anomalyTypes')}],
    ensure_ascii=False) if zero else "无")
print("  非零样例:", json.dumps(
    [{k: recs[i][k] for k in ('teachingClassId', 'scoredCount', 'passRate', 'baselinePassRate', 'anomalyTypes')}
     for i in range(min(3, len(recs)))], ensure_ascii=False))

# 2) 毕业风险：按年级看 remainingSemesters
print("\n[毕业风险 remainingSemesters]")
grade_expect = {1: ("2022级", "大四/已毕业窗口", 2), 2: ("2023级", "大四上", 2),
                3: ("2024级", "大三上", 4), 4: ("2025级", "大二上", 6), 5: ("2026级", "大一上", 8)}
for gid, (gname, stage, expect) in grade_expect.items():
    st, js = call("GET", f"/api/v1/admin/analytics/graduation-risks?semesterId=5&gradeId={gid}&page=1&pageSize=3", admin)
    d = js.get("data") or {}
    rs = d.get("records") or []
    if not rs:
        print(f"  {gname}: 无记录")
        continue
    r = rs[0]
    print(f"  {gname}({stage}) 应为 {expect} 学期 -> API remainingSemesters={r['remainingSemesters']}, "
          f"riskLevel={r['riskLevel']}, progressRate={r['progressRate']}, requiredRate={r.get('requiredRate')}")
    print(f"      样例: {r['studentNo']} {r['studentName']} 已完成 {r['completedCredit']}/{r['planTotalCredit']}")

# 3) 毕业风险分布（各年级 riskLevel 计数）
print("\n[毕业风险 年级 x riskLevel]")
for level in ("HIGH", "MEDIUM", "LOW"):
    st, js = call("GET", f"/api/v1/admin/analytics/graduation-risks?semesterId=5&riskLevel={level}&page=1&pageSize=1", admin)
    print(f"  riskLevel={level} total={js['data']['total']}")

# 4) 成绩异常按类型过滤
print("\n[成绩异常 按 anomalyType 过滤]")
for t in ("FAIL_RATE_HIGH", "SCORE_TOO_HIGH", "SCORE_MISSING"):
    st, js = call("GET", f"/api/v1/admin/analytics/score-anomalies?semesterId=5&anomalyType={t}&page=1&pageSize=1", admin)
    print(f"  {t}: total={js['data']['total']}")

# 5) 选课统计 total / 教室利用率
st, js = call("GET", "/api/v1/admin/statistics/course-selection?semesterId=5&page=1&pageSize=1", admin)
print("\n[选课统计] total=", js["data"]["total"])
st, js = call("GET", "/api/v1/admin/statistics/classroom-usage?semesterId=5&page=1&pageSize=100", admin)
rates = sorted(x["usageRate"] for x in js["data"]["records"])
print("[教室利用率] n=%d min=%.2f median=%.2f max=%.2f" % (
    len(rates), rates[0], rates[len(rates)//2], rates[-1]))

# 6) 教师工作量梯度
st, js = call("GET", "/api/v1/admin/statistics/teacher-workload?semesterId=5&page=1&pageSize=200", admin)
hrs = sorted(x["totalHours"] for x in js["data"]["records"])
print("[教师工作量] n=%d min=%d median=%d max=%d zero=%d" % (
    len(hrs), hrs[0], hrs[len(hrs)//2], hrs[-1], sum(1 for h in hrs if h == 0)))

# 7) 选课热度预测 扩容建议分布
st, js = call("GET", "/api/v1/admin/analytics/enrollment-forecast?semesterId=5", admin)
recs = js["data"]["records"]
print("\n[选课热度预测] n=%d" % len(recs))
sug = Counter(r.get("suggestType") or r.get("recommendation") or "?" for r in recs)
print("  建议类型分布:", dict(sug))
print("  样例:", json.dumps(recs[0], ensure_ascii=False)[:800])
