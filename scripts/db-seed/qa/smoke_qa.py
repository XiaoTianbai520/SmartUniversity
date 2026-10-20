"""回归冒烟（替代缺失的 target/smoke.py、target/smoke_admin.py）

覆盖：登录鉴权、改名账号、学生/教师/教务核心只读链路。
用法：python smoke_qa.py [base_url]
"""
import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080"
RESULTS = []


def call(method, path, token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body else None
    h = {"Accept": "application/json"}
    if body:
        h["Content-Type"] = "application/json; charset=utf-8"
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:  # noqa: BLE001
            return e.code, {"raw": raw}
    except Exception as e:  # noqa: BLE001
        return -1, {"raw": str(e)}


def check(name, cond, detail=""):
    RESULTS.append({"name": name, "pass": bool(cond), "detail": detail})
    print(f"[{'PASS' if cond else 'FAIL'}] {name}" + (f"  {detail}" if detail else ""))


def login(u, p, t):
    st, js = call("POST", "/api/v1/auth/login", body={"username": u, "password": p, "loginType": t})
    return st, js


def main():
    # ---------- 1. 登录 / 改名账号 ----------
    st, js = login("admin", "123456", "ADMIN")
    admin = (js.get("data") or {}).get("token")
    check("管理员 admin 登录", st == 200 and js.get("code") == 200 and admin, f"HTTP={st}")
    check("管理员显示名非空", bool((js.get("data") or {}).get("displayName")),
          str((js.get("data") or {}).get("displayName")))

    expect_names = {"20260001": "张明远", "20260002": "李思远", "20260003": "王雨桐"}
    stu_tokens = {}
    for no, expect in expect_names.items():
        st, js = login(no, "123456", "STUDENT")
        tok = (js.get("data") or {}).get("token")
        stu_tokens[no] = tok
        dn = (js.get("data") or {}).get("displayName")
        check(f"学生 {no} 登录", st == 200 and js.get("code") == 200 and bool(tok), f"HTTP={st}")
        check(f"学生 {no} 显示名 == {expect}", dn == expect, f"实际 {dn}")

    st, js = login("T10001", "123456", "TEACHER")
    tea = (js.get("data") or {}).get("token")
    check("教师 T10001 登录", st == 200 and js.get("code") == 200 and bool(tea), f"HTTP={st}")
    check("教师 T10001 显示名 == 张启明", (js.get("data") or {}).get("displayName") == "张启明",
          str((js.get("data") or {}).get("displayName")))

    # ---------- 2. 鉴权负向 ----------
    st, js = call("GET", "/api/v1/admin/dashboard/overview")
    check("无 token 访问管理端被拒", st != 200 or js.get("code") not in (200,), f"HTTP={st} code={js.get('code')}")
    st, js = call("GET", "/api/v1/admin/dashboard/overview", stu_tokens.get("20260001"))
    check("学生 token 访问管理端被拒", js.get("code") not in (200,), f"code={js.get('code')}")

    # ---------- 3. 教务端基础数据 ----------
    for name, path in [
        ("学期列表", "/api/v1/admin/semesters?page=1&pageSize=50"),
        ("选课批次列表", "/api/v1/admin/selection-batches?page=1&pageSize=30"),
        ("课程列表", "/api/v1/admin/courses?page=1&pageSize=20"),
        ("教学班列表", "/api/v1/admin/teaching-classes?page=1&pageSize=20&semesterId=5"),
        ("教室列表", "/api/v1/admin/classrooms?page=1&pageSize=20"),
        ("教师列表", "/api/v1/admin/teachers?page=1&pageSize=20"),
        ("学生列表", "/api/v1/admin/students?page=1&pageSize=20"),
        ("专业列表", "/api/v1/admin/majors?page=1&pageSize=30"),
        ("年级列表", "/api/v1/admin/grades?page=1&pageSize=30"),
        ("定时任务列表", "/api/v1/admin/schedule-tasks"),
    ]:
        st, js = call("GET", path, admin)
        check(f"教务端 {name}", st == 200 and js.get("code") == 200, f"HTTP={st} code={js.get('code')}")

    # ---------- 4. 学生端链路 ----------
    for no, tok in stu_tokens.items():
        for name, path in [
            ("可选教学班", "/api/v1/student/teaching-classes?page=1&pageSize=10"),
            ("当前批次", "/api/v1/student/batch"),
            ("我的选课", "/api/v1/student/selections?page=1&pageSize=10"),
            ("候补列表", "/api/v1/student/waitlist?page=1&pageSize=10"),
            ("我的成绩", "/api/v1/student/scores"),
            ("我的课表", "/api/v1/student/timetable"),
            ("学分汇总", "/api/v1/student/credits"),
            ("学分进度", "/api/v1/student/credit-progress"),
            ("毕业风险", "/api/v1/student/graduation-risk"),
        ]:
            st, js = call("GET", path, tok)
            check(f"学生 {no} {name}", st == 200 and js.get("code") == 200,
                  f"HTTP={st} code={js.get('code')}")

    # ---------- 5. 教师端链路（T10005 当前学期有课） ----------
    st, js = login("T10005", "123456", "TEACHER")
    t5 = (js.get("data") or {}).get("token")
    check("教师 T10005 登录", bool(t5), f"code={js.get('code')}")
    if t5:
        st, js = call("GET", "/api/v1/teacher/teaching-classes", t5)
        cls = js.get("data") or []
        check("教师 T10005 教学班非空", st == 200 and js.get("code") == 200 and len(cls) > 0,
              f"n={len(cls)}")
        if cls:
            cid = cls[0].get("teachingClassId") or cls[0].get("id")
            for name, path in [
                ("教学班详情", f"/api/v1/teacher/teaching-classes/{cid}"),
                ("教学班学生名单", f"/api/v1/teacher/teaching-classes/{cid}/students?page=1&pageSize=10"),
                ("成绩列表", f"/api/v1/teacher/teaching-classes/{cid}/scores"),
                ("成绩统计", f"/api/v1/teacher/teaching-classes/{cid}/score-statistics"),
            ]:
                st, js = call("GET", path, t5)
                check(f"教师端 {name}", st == 200 and js.get("code") == 200,
                      f"HTTP={st} code={js.get('code')}")
        for name, path in [("教师工作量", "/api/v1/teacher/workload"),
                           ("成绩预警", "/api/v1/teacher/score-warnings")]:
            st, js = call("GET", path, t5)
            check(f"教师端 {name}", st == 200 and js.get("code") == 200, f"HTTP={st} code={js.get('code')}")

    # ---------- 6. 通知 ----------
    # 注：学期归档仅有 POST /admin/semesters/{id}/archive，无查询接口，此处不做 GET 校验
    for name, path in [("通知列表", "/api/v1/notifications?page=1&pageSize=10")]:
        st, js = call("GET", path, admin)
        check(f"{name}", st == 200 and js.get("code") in (200, 40004), f"HTTP={st} code={js.get('code')}")

    ok = sum(1 for r in RESULTS if r["pass"])
    bad = [r for r in RESULTS if not r["pass"]]
    print(f"\n合计 {len(RESULTS)} 项，PASS {ok}，FAIL {len(bad)}")
    for r in bad:
        print("  FAIL:", r["name"], r["detail"])
    with open("smoke_qa_result.json", "w", encoding="utf-8") as f:
        json.dump(RESULTS, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
