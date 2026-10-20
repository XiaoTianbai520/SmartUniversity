# -*- coding: utf-8 -*-
"""任务#3：成绩真实化 + 异常样本植入。

模型：
    score = clamp(round(75 + course_diff + ability + noise, 1), 0, 100)
      course_diff : 课程稳定难度偏移，[-10, +10]（同一课程跨学期一致）
      ability     : 学生能力 N(0,5)；约 12% 学业困难群体额外 -6
      noise       : N(0,6)

状态规则：
  已结束学期(2022-2023-1 ~ 2025-2026-2，共 8 个)：全部 PUBLISHED，published_at 在学期结束后一周内
  当前学期 2026-2027-1：按教学班 15% PUBLISHED / 60% UNPUBLISHED / 25% 不生成 score 行
异常样本：
  * sem5 一个 >=40 人的班：均分 52、标准差 6（FAIL_RATE_HIGH）
  * sem5 另一个班：均分 93、标准差 3（SCORE_TOO_HIGH）
  * 2025-2026-2 一个班：5 名 SELECTED 学生不生成 score 行（SCORE_MISSING）
选中 id 写入 _gen/anomaly_samples.json

输出：scripts/db-seed/_gen/05_scores.sql
"""
import hashlib
import json
import os
import random
import sys
from datetime import datetime, timedelta

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common

SEED = 20261020
BASE = 75.5
COURSE_AMP = 12.0
ABILITY_SD = 5.0
HARDSHIP_P = 0.12
HARDSHIP_VAL = -6.0
NOISE_SD = 7.0

CURRENT_SEM_CODE = "2026-2027-1"
MISSING_SEM_CODE = "2025-2026-2"


def stable_unit(*parts):
    raw = "|".join(str(p) for p in parts).encode("utf-8")
    return int(hashlib.md5(raw).hexdigest()[:12], 16) / float(16 ** 12)


def clamp(v, lo=0.0, hi=100.0):
    return max(lo, min(hi, v))


def main():
    rng = random.Random(SEED)
    # ---------- 学期信息 ----------
    sem_rows = common.query(
        "SELECT id,semester_code,end_date,status FROM semester ORDER BY start_date;")
    sem = {}
    for r in sem_rows:
        sid = int(r[0])
        sem[sid] = dict(id=sid, code=r[1],
                        end_date=datetime.strptime(r[2], "%Y-%m-%d"), status=r[3])
    cur_sem_id = [k for k, v in sem.items() if v["code"] == CURRENT_SEM_CODE][0]
    miss_sem_id = [k for k, v in sem.items() if v["code"] == MISSING_SEM_CODE][0]

    # ---------- 学生能力 ----------
    stu_rows = common.query("SELECT id FROM student ORDER BY id;")
    ability = {}
    hardship = set()
    for r in stu_rows:
        sid = int(r[0])
        s_rng = random.Random(SEED * 31 + sid)
        a = s_rng.gauss(0.0, ABILITY_SD)
        if stable_unit("hardship", sid) < HARDSHIP_P:
            a += HARDSHIP_VAL
            hardship.add(sid)
        ability[sid] = a

    # ---------- 所有 SELECTED 选课 ----------
    sel_rows = common.query(
        "SELECT cs.id, cs.student_id, tc.course_id, tc.semester_id, tc.id "
        "FROM course_selection cs JOIN teaching_class tc ON tc.id = cs.teaching_class_id "
        "WHERE cs.status = 'SELECTED' ORDER BY cs.id;")
    selections = [(int(r[0]), int(r[1]), int(r[2]), int(r[3]), int(r[4])) for r in sel_rows]
    print("[load] SELECTED 选课=%d" % len(selections))

    # 班级 -> 选课行
    by_class = {}
    for (csid, stu, course, sid, tc) in selections:
        by_class.setdefault(tc, []).append(csid)

    # ---------- 挑选异常样本 ----------
    class_sel_cnt = {tc: len(v) for tc, v in by_class.items()}
    cur_classes = [tc for (csid, stu, course, sid, tc) in selections if sid == cur_sem_id]
    cur_classes = sorted(set(cur_classes))
    big_classes = [tc for tc in cur_classes if class_sel_cnt[tc] >= 40]
    if len(big_classes) < 2:
        raise SystemExit("当前学期 >=40 人的教学班不足 2 个，无法植入双异常样本")
    fail_class = big_classes[0]
    high_class = big_classes[1]
    miss_classes = sorted({tc for (csid, stu, course, sid, tc) in selections
                           if sid == miss_sem_id and class_sel_cnt[tc] >= 10})
    miss_class = miss_classes[0]
    miss_students = sorted({stu for (csid, stu, course, sid, tc) in selections
                            if tc == miss_class})[:5]

    anomaly = dict(
        fail_rate_high_class=fail_class, fail_rate_high_semester=cur_sem_id,
        score_too_high_class=high_class, score_too_high_semester=cur_sem_id,
        score_missing_class=miss_class, score_missing_semester=miss_sem_id,
        score_missing_students=miss_students)
    print("[anomaly] %s" % json.dumps(anomaly, ensure_ascii=False))

    # ---------- 课程难度偏移（稳定） ----------
    course_diff = {}

    def get_diff(course_id):
        d = course_diff.get(course_id)
        if d is None:
            d = (stable_unit("diff", course_id) * 2.0 - 1.0) * COURSE_AMP
            course_diff[course_id] = d
        return d

    # ---------- sem5 班级状态桶 ----------
    bucket = {}
    for tc in cur_classes:
        u = stable_unit("bucket", tc)
        if u < 0.15:
            bucket[tc] = "PUBLISHED"
        elif u < 0.75:
            bucket[tc] = "UNPUBLISHED"
        else:
            bucket[tc] = "NONE"
    bucket[fail_class] = "UNPUBLISHED"   # 异常样本：成绩已录入但未发布
    bucket[high_class] = "UNPUBLISHED"

    # ---------- 生成成绩 ----------
    now_dt = datetime(2026, 10, 20, 10, 0, 0)
    rows = []
    sampled_values = []
    for (csid, stu, course, sid, tc) in selections:
        # 缺分样本
        if tc == miss_class and stu in miss_students:
            continue
        if sid == cur_sem_id and bucket.get(tc) == "NONE":
            continue

        if tc == fail_class:
            val = clamp(round(rng.gauss(52.0, 6.0), 1))
        elif tc == high_class:
            val = clamp(round(rng.gauss(93.0, 3.0), 1))
        else:
            val = clamp(round(BASE + get_diff(course) + ability[stu] + rng.gauss(0.0, NOISE_SD), 1))

        if sid == cur_sem_id:
            status = bucket.get(tc, "UNPUBLISHED")
            pub = now_dt if status == "PUBLISHED" else None
        else:
            status = "PUBLISHED"
            pub = sem[sid]["end_date"] + timedelta(
                days=rng.uniform(0.2, 6.5))
        rows.append((csid, val, status, pub.strftime("%Y-%m-%d %H:%M:%S") if pub else None))
        if tc not in (fail_class, high_class):
            sampled_values.append(float(val))

    # ---------- 统计 ----------
    vals = sampled_values
    n = len(vals)
    mean = sum(vals) / n
    var = sum((v - mean) ** 2 for v in vals) / n
    sd = var ** 0.5
    pass_rate = sum(1 for v in vals if v >= 60) / n
    exc_rate = sum(1 for v in vals if v >= 85) / n
    buckets = {"0-59": 0, "60-69": 0, "70-79": 0, "80-89": 0, "90-100": 0}
    for v in vals:
        if v < 60:
            buckets["0-59"] += 1
        elif v < 70:
            buckets["60-69"] += 1
        elif v < 80:
            buckets["70-79"] += 1
        elif v < 90:
            buckets["80-89"] += 1
        else:
            buckets["90-100"] += 1
    print("[dist] 常规成绩 n=%d mean=%.2f sd=%.2f pass=%.2f%% excellent=%.2f%%"
          % (n, mean, sd, pass_rate * 100, exc_rate * 100))
    print("[dist] 分段:", buckets)

    # ---------- 写 SQL ----------
    cols = ["course_selection_id", "score_value", "status", "published_at"]
    stmts = ["DELETE FROM score;"]
    stmts += common.batch_insert("score", cols, rows, 500)
    path = common.write_sql("05_scores.sql", stmts, header="成绩真实化（task#3）")
    print("[write] %s rows=%d" % (path, len(rows)))
    common.run_file(path)

    os.makedirs(common.GEN_DIR, exist_ok=True)
    with open(os.path.join(common.GEN_DIR, "anomaly_samples.json"), "w", encoding="utf-8") as fh:
        json.dump(anomaly, fh, ensure_ascii=False, indent=2)

    # ---------- 校验（DB 实测） ----------
    chk = common.query(
        "SELECT "
        " SUM(score_value<60), SUM(score_value>=60 AND score_value<70), "
        " SUM(score_value>=70 AND score_value<80), SUM(score_value>=80 AND score_value<90), "
        " SUM(score_value>=90), COUNT(*), ROUND(AVG(score_value),2), ROUND(STDDEV(score_value),2) "
        "FROM score;")
    print("[db] 全部分段/总数/均值/标准差:", chk)
    chk2 = common.query(
        "SELECT status, COUNT(*) FROM score GROUP BY status;")
    print("[db] 状态:", chk2)
    print("[db] sem5 PUBLISHED/UNPUBLISHED/NONE 班数:",
          sum(1 for v in bucket.values() if v == "PUBLISHED"),
          sum(1 for v in bucket.values() if v == "UNPUBLISHED"),
          sum(1 for v in bucket.values() if v == "NONE"))


if __name__ == "__main__":
    main()
