#!/usr/bin/env bash
# 修复后统一重跑：9 学期快照 → 巡检 → 8 学期归档 → 关键接口实测
set -u
BASE="http://127.0.0.1:8081/api/v1"
PY="C:/Users/c1338/.workbuddy/binaries/python/versions/3.13.12/python.exe"
LOG="scripts/db-seed/_gen/task9_rerun.log"
: > "$LOG"

login() {
  curl -s --noproxy '*' --max-time 30 -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"123456\",\"loginType\":\"$2\"}" \
    | "$PY" -c "import sys,json;print((json.load(sys.stdin).get('data') or {}).get('token',''))"
}
j() { "$PY" -c "import sys,json
try:
 d=json.load(sys.stdin); print('code=%s msg=%s data=%s'%(d.get('code'),d.get('message'),d.get('data')))
except Exception: print('RAW')"; }

echo "=== 统计快照刷新（9 学期）===" | tee -a "$LOG"
for pair in "9:2022-2023-1" "10:2022-2023-2" "11:2023-2024-1" "12:2023-2024-2" "1:2024-2025-1" "2:2024-2025-2" "3:2025-2026-1" "4:2025-2026-2" "5:2026-2027-1"; do
  sid=${pair%%:*}; code=${pair##*:}
  tok=$(login admin ADMIN)
  t0=$(date +%s)
  body=$(curl -s --noproxy '*' --max-time 400 -X POST "$BASE/admin/statistics/snapshots/refresh?semesterId=$sid" -H "Authorization: Bearer $tok")
  t1=$(date +%s)
  echo "  sem=$sid $code ($((t1-t0))s) => $(echo "$body" | j)" | tee -a "$LOG"
done

echo "=== 数据巡检 ===" | tee -a "$LOG"
tok=$(login admin ADMIN)
body=$(curl -s --noproxy '*' --max-time 400 -X POST "$BASE/admin/schedule-tasks/DATA_AUDIT_TASK/run" -H "Authorization: Bearer $tok")
echo "  $(echo "$body" | j)" | tee -a "$LOG"

echo "=== 学期归档（8 个已结束）===" | tee -a "$LOG"
for pair in "9:2022-2023-1" "10:2022-2023-2" "11:2023-2024-1" "12:2023-2024-2" "1:2024-2025-1" "2:2024-2025-2" "3:2025-2026-1" "4:2025-2026-2"; do
  sid=${pair%%:*}; code=${pair##*:}
  tok=$(login admin ADMIN)
  body=$(curl -s --noproxy '*' --max-time 400 -X POST "$BASE/admin/semesters/$sid/archive?archiveTypes=SELECTION,SCORE,OVERVIEW" -H "Authorization: Bearer $tok")
  echo "  sem=$sid $code => $(echo "$body" | j)" | tee -a "$LOG"
done

echo "=== score-anomalies sem5 ===" | tee -a "$LOG"
tok=$(login admin ADMIN)
curl -s --noproxy '*' --max-time 200 "$BASE/admin/analytics/score-anomalies?semesterId=5&pageNo=1&pageSize=200" -H "Authorization: Bearer $tok" \
 | "$PY" -c "
import sys,json
d=json.load(sys.stdin); dt=d.get('data') or {}
lst=dt.get('list') or dt.get('records') or []
ids={r.get('teachingClassId'):r.get('anomalyTypes') for r in lst}
print('code=%s total=%s len=%s has739=%s has759=%s'%(d.get('code'),dt.get('total'),len(lst),739 in ids,759 in ids))
" | tee -a "$LOG"

echo "=== graduation-risks 剩余学期（按 gradeId）===" | tee -a "$LOG"
tok=$(login admin ADMIN)
for g in "1:2022级" "2:2023级" "3:2024级" "4:2025级" "5:2026级"; do
  gid=${g%%:*}; gname=${g##*:}
  curl -s --noproxy '*' --max-time 200 "$BASE/admin/analytics/graduation-risks?semesterId=5&gradeId=$gid&currentPage=1&limit=5" -H "Authorization: Bearer $tok" \
   | "$PY" -c "
import sys,json
d=json.load(sys.stdin); dt=d.get('data') or {}; recs=dt.get('records') or []
rs=sorted({r.get('remainingSemesters') for r in recs})
print('  grade=$gid $gname remainingSemesters=%s sampleSize=%s'%(rs,len(recs)))
" | tee -a "$LOG"
done

echo "=== warnings/credit 风险分布 sem5 ===" | tee -a "$LOG"
tok=$(login admin ADMIN)
curl -s --noproxy '*' --max-time 200 "$BASE/admin/warnings/credit?semesterId=5&currentPage=1&limit=1" -H "Authorization: Bearer $tok" \
 | "$PY" -c "
import sys,json
d=json.load(sys.stdin); dt=d.get('data') or {}
print('  code=%s total=%s extra=%s'%(d.get('code'),dt.get('total'),json.dumps(dt.get('extra'),ensure_ascii=False)))
" | tee -a "$LOG"
echo "[done]" | tee -a "$LOG"
