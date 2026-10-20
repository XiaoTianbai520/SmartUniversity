# -*- coding: utf-8 -*-
"""任务#2（第二部分）：为 6 个空学期生成逐学期、逐学生的选课历史。

规则：
  * 每名学生在「入学学期 ~ 2026-2027-1」逐学期选课，但 sem3/sem4/sem5 已有数据不动；
  * 目标学分按在读年级：大一26~30 大二24~28 大三22~26 大四16~22 大五12~16；
  * 先全选 REQUIRED，再 MAJOR_ELECTIVE，再 PUBLIC_ELECTIVE，累加到目标学分，且 ≤30；
  * 状态分布 92% SELECTED / 5% WITHDRAWN / 3% WAITING（waitlist_no 班内递增）；
  * 只选 status<>'CANCELLED' 且 major/grade 适用范围覆盖该生的教学班；同课只选一个班。

输出：scripts/db-seed/_gen/04_selections.sql
"""
import hashlib
import math
import os
import random
import sys
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common

TARGET_CODES = [
    "2022-2023-1", "2022-2023-2", "2023-2024-1",
    "2023-2024-2", "2024-2025-1", "2024-2025-2",
]
CURRENT_SEM_CODE = "2026-2027-1"
CREDIT_RANGE = {1: (26, 30), 2: (24, 28), 3: (22, 26), 4: (16, 22), 5: (12, 16)}


def stable_unit(*parts):
    raw = "|".join(str(p) for p in parts).encode("utf-8")
    return int(hashlib.md5(raw).hexdigest()[:12], 16) / float(16 ** 12)


def fmt_dt(dt):
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def main():
    # ---------- 学期 ----------
    rows = common.query(
        "SELECT id,semester_code,academic_year,batch_id FROM ("
        "  SELECT s.id, s.semester_code, s.academic_year, "
        "         (SELECT b.id FROM selection_batch b WHERE b.semester_id=s.id "
        "           ORDER BY b.id LIMIT 1) AS batch_id "
        "  FROM semester s) t WHERE semester_code IN (%s) ORDER BY id;"
        % ",".join(common.sql_escape(c) for c in TARGET_CODES + [CURRENT_SEM_CODE])
    )
    sem_info = {}
    for r in rows:
        sid = int(r[0])
        sem_info[r[1]] = dict(id=sid, code=r[1], academic_year=int(r[2][:4]),
                              batch_id=int(r[3]) if r[3] else None)

    # 批次时间窗
    b_rows = common.query(
        "SELECT id,start_time,end_time,drop_deadline FROM selection_batch "
        "WHERE id IN (%s);" % ",".join(str(s["batch_id"]) for s in sem_info.values()))
    batch_win = {}
    for r in b_rows:
        batch_win[int(r[0])] = (
            datetime.strptime(r[1], "%Y-%m-%d %H:%M:%S"),
            datetime.strptime(r[2], "%Y-%m-%d %H:%M:%S"),
            datetime.strptime(r[3], "%Y-%m-%d %H:%M:%S") if r[3] else None,
        )

    target_ids = [sem_info[c]["id"] for c in TARGET_CODES]
    id_csv = ",".join(str(i) for i in target_ids)

    # ---------- 学生 ----------
    stu_rows = common.query(
        "SELECT s.id, s.major_id, g.entry_year "
        "FROM student s JOIN grade_cohort g ON g.id = s.grade_id WHERE s.status = 1;")
    students = [(int(r[0]), int(r[1]), int(r[2])) for r in stu_rows]

    # ---------- 目标学期教学班（含适用范围） ----------
    tc_rows = common.query(
        "SELECT tc.id, tc.semester_id, tc.course_id, c.course_type, c.credit, "
        "       tcm.major_id, tc.capacity "
        "FROM teaching_class tc "
        "JOIN course c ON c.id = tc.course_id "
        "JOIN teaching_class_major tcm ON tcm.teaching_class_id = tc.id "
        "WHERE tc.semester_id IN (%s) AND tc.status <> 'CANCELLED';" % id_csv)
    # idx[sem][major][course_id] = dict(type, credit, classes=[...], cap=class_cap_map)
    idx = {}
    class_cap = {}
    for r in tc_rows:
        tc_id, sid, course_id, ctype, credit, major_id, cap = (
            int(r[0]), int(r[1]), int(r[2]), r[3], float(r[4]), int(r[5]), int(r[6]))
        class_cap[tc_id] = cap
        d = idx.setdefault(sid, {}).setdefault(major_id, {})
        e = d.get(course_id)
        if e is None:
            e = dict(course_id=course_id, ctype=ctype, credit=credit, classes=[])
            d[course_id] = e
        e["classes"].append(tc_id)

    # ---------- 学分可行性诊断 ----------
    print("[diag] 每专业「全选必修」总学分（取最大）:")
    max_req = 0
    for sid in target_ids:
        for major_id, courses in idx.get(sid, {}).items():
            req = sum(e["credit"] for e in courses.values() if e["ctype"] == "REQUIRED")
            max_req = max(max_req, req)
    print("       max required credits = %.1f" % max_req)
    if max_req > 30.0:
        print("       [WARN] 部分专业必修学分 > 30，将按 30 上限截断")

    # ---------- 生成 ----------
    sel_rows = []          # (student_id, tc_id, batch_id, status, waitlist_no, selected_at, withdrawn_at)
    sel_meta = []          # 与 sel_rows 对齐，记录 sem_id 用于后续统计
    per_sem_cnt = {sid: 0 for sid in target_ids}
    credit_stat = {}
    overwrite_warn = 0

    for (stu_id, major_id, entry_year) in students:
        for code in TARGET_CODES:
            sem = sem_info[code]
            sid = sem["id"]
            year_level = sem["academic_year"] - entry_year + 1
            if year_level < 1 or year_level > 5:
                continue
            courses = idx.get(sid, {}).get(major_id)
            if not courses:
                continue
            lo, hi = CREDIT_RANGE[year_level]
            rng = random.Random((stu_id * 1315423911) ^ (sid * 2654435761))
            target_credit = rng.uniform(lo, hi)

            required = [e for e in courses.values() if e["ctype"] == "REQUIRED"]
            major_el = [e for e in courses.values() if e["ctype"] == "MAJOR_ELECTIVE"]
            public_el = [e for e in courses.values() if e["ctype"] == "PUBLIC_ELECTIVE"]

            chosen = []
            credits = 0.0

            def take(e):
                nonlocal credits
                if credits + e["credit"] > 30.0 + 1e-6:
                    return False
                chosen.append(e)
                credits += e["credit"]
                return True

            # 1) 全选必修
            for e in sorted(required, key=lambda x: x["course_id"]):
                take(e)

            # 2) 加权随机排序（热门课权重高）
            def weighted_order(pool):
                keyed = []
                for e in pool:
                    w = 0.25 + stable_unit("pop", e["course_id"]) * 1.75
                    u = max(rng.random(), 1e-9)
                    keyed.append(((-math.log(u)) / w, e))
                keyed.sort(key=lambda x: x[0])
                return [e for _, e in keyed]

            # 3) 专业选修 → 公共选修，累加到目标学分
            for pool in (weighted_order(major_el), weighted_order(public_el)):
                for e in pool:
                    if credits >= target_credit:
                        break
                    take(e)

            # 4) 不足 4 门时继续补
            if len(chosen) < 4:
                for e in weighted_order(major_el + public_el):
                    if len(chosen) >= 4:
                        break
                    if e not in chosen:
                        take(e)

            if len(chosen) < 4:
                continue

            # 5) 状态与时间
            bstart, bend, bdrop = batch_win[sem["batch_id"]]
            span = max(1, int((bend - bstart).total_seconds()))
            for e in chosen:
                tc_id = e["classes"][rng.randrange(len(e["classes"]))]
                sec = rng.randint(0, span - 1)
                sel_at = datetime.fromtimestamp(bstart.timestamp() + sec)
                rr = rng.random()
                if rr < 0.92:
                    status, wait_no, wd_at = "SELECTED", None, None
                elif rr < 0.97:
                    status, wait_no = "WITHDRAWN", None
                    wd_base = bdrop if bdrop and bdrop > sel_at else bend
                    wd_at = datetime.fromtimestamp(
                        sel_at.timestamp() + rng.uniform(0, max(1, (wd_base - sel_at).total_seconds())))
                else:
                    status, wait_no, wd_at = "WAITING", -1, None  # 占位，稍后编号
                sel_rows.append((stu_id, tc_id, sem["batch_id"], status,
                                 wait_no, fmt_dt(sel_at),
                                 fmt_dt(wd_at) if wd_at else None))
                sel_meta.append((sid, credits))
                per_sem_cnt[sid] += 1
                credit_stat.setdefault(sid, []).append(credits)

    # ---------- WAITING 候补序号：同班内按 selected_at 递增 ----------
    waiting = [(i, row) for i, row in enumerate(sel_rows) if row[3] == "WAITING"]
    waiting.sort(key=lambda x: (x[1][1], x[1][5]))
    counter = {}
    for i, row in waiting:
        tc = row[1]
        counter[tc] = counter.get(tc, 0) + 1
        sel_rows[i] = (row[0], row[1], row[2], row[3], counter[tc], row[5], row[6])

    # ---------- 写 SQL ----------
    cols = ["student_id", "teaching_class_id", "batch_id", "status",
            "waitlist_no", "selected_at", "withdrawn_at"]
    stmts = ["DELETE cs FROM course_selection cs JOIN teaching_class tc "
             "ON tc.id = cs.teaching_class_id WHERE tc.semester_id IN (%s);" % id_csv]
    stmts += common.batch_insert("course_selection", cols, sel_rows, 500)
    path = common.write_sql("04_selections.sql", stmts, header="生成选课历史（task#2 part2）")
    print("[write] %s rows=%d" % (path, len(sel_rows)))
    common.run_file(path)

    # ---------- 校验 ----------
    print("\n[sem] 学期  学生数  选课数  人均  平均学分(去重前)")
    for code in TARGET_CODES:
        sid = sem_info[code]["id"]
        n = per_sem_cnt[sid]
        cr = credit_stat.get(sid, [])
        avg_cr = sum(cr) / len(cr) if cr else 0
        stu_n = len({r[0] for r, m in zip(sel_rows, sel_meta) if m[0] == sid})
        print("  %-13s %5d %6d  %.2f  %.2f" % (code, stu_n, n,
                                               n / stu_n if stu_n else 0, avg_cr))

    status_cnt = common.query(
        "SELECT cs.status, COUNT(*) FROM course_selection cs "
        "JOIN teaching_class tc ON tc.id = cs.teaching_class_id "
        "WHERE tc.semester_id IN (%s) GROUP BY cs.status;" % id_csv)
    print("[status] 新增选课状态分布:", status_cnt)

    # over-capacity preview
    over = common.query(
        "SELECT tc.id, tc.class_code, tc.capacity, "
        " (SELECT COUNT(*) FROM course_selection cs WHERE cs.teaching_class_id=tc.id AND cs.status='SELECTED') sel "
        "FROM teaching_class tc WHERE tc.semester_id IN (%s) "
        "HAVING sel > tc.capacity ORDER BY sel - tc.capacity DESC LIMIT 8;" % id_csv)
    print("[hot] 超容量 Top8 (id,code,cap,selected):")
    for r in over:
        print("   ", r)


if __name__ == "__main__":
    main()
