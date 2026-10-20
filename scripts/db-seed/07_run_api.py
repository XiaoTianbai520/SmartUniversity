# -*- coding: utf-8 -*-
"""任务#5：调用管理端接口重跑统计快照 / 数据巡检 / 学期归档。

前置：应用已在 127.0.0.1:8081 启动。
使用 urllib（禁用系统代理），无第三方依赖。
"""
import json
import sys
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8081/api/v1"
# 有数据的 9 个学期（按时间顺序）：2022-2023-1 .. 2026-2027-1
SEM_MAP = [
    (9, "2022-2023-1", True),
    (10, "2022-2023-2", True),
    (11, "2023-2024-1", True),
    (12, "2023-2024-2", True),
    (1, "2024-2025-1", True),
    (2, "2024-2025-2", True),
    (3, "2025-2026-1", True),
    (4, "2025-2026-2", True),
    (5, "2026-2027-1", False),  # 当前学期不归档
]

_OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def call(method, path, token=None, body=None, timeout=180):
    url = BASE + path
    data = None
    headers = {"Content-Type": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with _OPENER.open(req, timeout=timeout) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {"raw": "unparseable"}
    except Exception as e:
        return -1, {"error": str(e)}


def main():
    status, resp = call("POST", "/auth/login", body={
        "username": "admin", "password": "123456", "loginType": "ADMIN"})
    token = resp.get("data", {}).get("token")
    if not token:
        print("登录失败:", status, resp)
        sys.exit(1)
    print("[login] ok code=%s user=%s" % (status, resp["data"].get("username")))

    # ---------- 1. 统计快照刷新 ----------
    print("\n=== 统计快照刷新 ===")
    for sid, code, _ in SEM_MAP:
        st, rp = call("POST", "/admin/statistics/snapshots/refresh?semesterId=%d" % sid, token)
        data = rp.get("data")
        summary = data if isinstance(data, (dict, str, int, list)) else ""
        print("  sem=%-2d %-12s http=%s code=%s msg=%s data=%s"
              % (sid, code, st, rp.get("code"), rp.get("message"), summary))

    # ---------- 2. 数据巡检 ----------
    print("\n=== 数据巡检任务 ===")
    st, rp = call("POST", "/admin/schedule-tasks/DATA_AUDIT_TASK/run", token)
    print("  http=%s code=%s msg=%s data=%s"
          % (st, rp.get("code"), rp.get("message"), rp.get("data")))

    # ---------- 3. 学期归档（仅已结束学期） ----------
    print("\n=== 学期归档 ===")
    for sid, code, finished in SEM_MAP:
        if not finished:
            print("  sem=%-2d %-12s 跳过(当前学期)" % (sid, code))
            continue
        st, rp = call("POST",
                      "/admin/semesters/%d/archive?archiveTypes=SELECTION,SCORE,OVERVIEW" % sid,
                      token)
        print("  sem=%-2d %-12s http=%s code=%s msg=%s data=%s"
              % (sid, code, st, rp.get("code"), rp.get("message"), rp.get("data")))


if __name__ == "__main__":
    main()
