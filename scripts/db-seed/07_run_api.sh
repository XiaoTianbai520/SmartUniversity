#!/usr/bin/env bash
# 任务#5：统计快照 / 数据巡检 / 学期归档（curl 实现；每次请求前重新登录取 token，规避 token 失效）
set -u
BASE="http://127.0.0.1:8081/api/v1"
PY="C:/Users/c1338/.workbuddy/binaries/python/versions/3.13.12/python.exe"

login() {
  curl -s --noproxy '*' --max-time 30 -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"123456","loginType":"ADMIN"}' \
    | "$PY" -c "import sys,json;print(json.load(sys.stdin)['data']['token'])"
}
show() { "$PY" -c "import sys,json
try:
    d=json.load(sys.stdin)
    print('code=%s msg=%s data=%s'%(d.get('code'),d.get('message'),d.get('data')))
except Exception:
    print('RAW')"; }

echo "=== 统计快照刷新 ==="
for pair in "9:2022-2023-1" "10:2022-2023-2" "11:2023-2024-1" "12:2023-2024-2" "1:2024-2025-1" "2:2024-2025-2" "3:2025-2026-1" "4:2025-2026-2" "5:2026-2027-1"; do
  sid=${pair%%:*}; code=${pair##*:}
  tok=$(login)
  t0=$(date +%s)
  body=$(curl -s --noproxy '*' --max-time 300 -X POST "$BASE/admin/statistics/snapshots/refresh?semesterId=$sid" -H "Authorization: Bearer $tok")
  t1=$(date +%s)
  echo "  sem=$sid $code ($((t1-t0))s) => $(echo "$body" | show)"
done

echo "=== 数据巡检任务 ==="
tok=$(login)
body=$(curl -s --noproxy '*' --max-time 300 -X POST "$BASE/admin/schedule-tasks/DATA_AUDIT_TASK/run" -H "Authorization: Bearer $tok")
echo "  $(echo "$body" | show)"

echo "=== 学期归档 ==="
for pair in "9:2022-2023-1" "10:2022-2023-2" "11:2023-2024-1" "12:2023-2024-2" "1:2024-2025-1" "2:2024-2025-2" "3:2025-2026-1" "4:2025-2026-2"; do
  sid=${pair%%:*}; code=${pair##*:}
  tok=$(login)
  body=$(curl -s --noproxy '*' --max-time 300 -X POST "$BASE/admin/semesters/$sid/archive?archiveTypes=SELECTION,SCORE,OVERVIEW" -H "Authorization: Bearer $tok")
  echo "  sem=$sid $code => $(echo "$body" | show)"
done
echo "[done]"
