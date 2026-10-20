# -*- coding: utf-8 -*-
"""任务#4：重算冗余计数 + 植入巡检异常样本（当前学期 2026-2027-1）。

  1) teaching_class.selected_count = 该班 SELECTED 选课数（DB 权威口径）
  2) 3 个满班样本：capacity = selected_count（30~60 人的班）
  3) 1 个超容量样本：capacity = selected_count - 3（>0）→ CAPACITY_ZERO
  4) 2 个无排课样本：删除其全部 teaching_class_schedule → NO_SCHEDULE
  5) 1 组排课冲突：2 个不同教学班同教室/同 weekday/节次与周次相交 → SCHEDULE_CONFLICT
结果写入 _gen/task4_samples.json
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common

CUR_SEM = 5  # 2026-2027-1


def main():
    # ---------- 1. 重算 selected_count ----------
    common.run_sql(
        "UPDATE teaching_class tc SET selected_count = ("
        "  SELECT COUNT(*) FROM course_selection cs "
        "   WHERE cs.teaching_class_id = tc.id AND cs.status = 'SELECTED');")
    tot = common.query_ints(
        "SELECT COUNT(*) FROM teaching_class WHERE selected_count <> ("
        "  SELECT COUNT(*) FROM course_selection cs "
        "   WHERE cs.teaching_class_id = teaching_class.id AND cs.status='SELECTED');")[0][0]
    print("[count] selected_count 已重算，口径不一致残留=%d" % tot)

    # 当前学期各班 SELECTED 人数
    rows = common.query(
        "SELECT tc.id, tc.class_code, tc.capacity, tc.teacher_id, tc.selected_count "
        "FROM teaching_class tc WHERE tc.semester_id = %d ORDER BY tc.id;" % CUR_SEM)
    classes = [dict(id=int(r[0]), code=r[1], cap=int(r[2]),
                    teacher=int(r[3]), sel=int(r[4])) for r in rows]

    used = set()
    # 排除已作为成绩异常样本的班级，避免语义混淆
    anomaly_file = os.path.join(common.GEN_DIR, "anomaly_samples.json")
    if os.path.exists(anomaly_file):
        with open(anomaly_file, encoding="utf-8") as fh:
            a = json.load(fh)
        used.add(a["fail_rate_high_class"])
        used.add(a["score_too_high_class"])

    # ---------- 2. 满班样本：30~60 人 ----------
    full = [c for c in classes if 30 <= c["sel"] <= 60 and c["id"] not in used]
    full.sort(key=lambda c: (-c["sel"], c["id"]))
    full_samples = full[:3]
    for c in full_samples:
        used.add(c["id"])

    # ---------- 3. 超容量样本：selected 15~45，capacity=sel-3 ----------
    over_cand = [c for c in classes if 15 <= c["sel"] <= 45 and c["id"] not in used]
    over_cand.sort(key=lambda c: (c["sel"], c["id"]))
    over_sample = over_cand[0]
    used.add(over_sample["id"])

    # ---------- 4. 无排课样本：2 个不同班 ----------
    no_sched = [c for c in classes if c["id"] not in used]
    no_sched.sort(key=lambda c: (c["sel"], c["id"]))
    no_sched_samples = no_sched[:2]
    used.update(c["id"] for c in no_sched_samples)

    # ---------- 5. 排课冲突样本：2 个不同班、不同教师 ----------
    conf_pool = [c for c in classes if c["id"] not in used]
    conf_a = conf_pool[0]
    conf_b = next(c for c in conf_pool[1:] if c["teacher"] != conf_a["teacher"])
    room_id = 1  # 统一用 1 号教室，weekday=6（周末）避开既有周一~周五排课

    # ---------- 写 SQL ----------
    stmts = []
    for c in full_samples:
        stmts.append("UPDATE teaching_class SET capacity = %d WHERE id = %d;"
                     % (c["sel"], c["id"]))
    stmts.append("UPDATE teaching_class SET capacity = %d WHERE id = %d;"
                 % (over_sample["sel"] - 3, over_sample["id"]))

    for c in no_sched_samples:
        stmts.append("DELETE FROM teaching_class_schedule WHERE teaching_class_id = %d;" % c["id"])

    # 冲突：各取第一条排课改为同教室/同 weekday6/第1-2节/1-16周
    for c in (conf_a, conf_b):
        stmts.append(
            "UPDATE teaching_class_schedule SET classroom_id=%d, weekday=6, "
            "start_section=1, end_section=2, start_week=1, end_week=16 "
            "WHERE id=(SELECT sid FROM (SELECT MIN(id) sid FROM teaching_class_schedule "
            "WHERE teaching_class_id=%d) t);" % (room_id, c["id"]))

    path = common.write_sql("06_finalize.sql", stmts, header="冗余计数与巡检样本（task#4）")
    print("[write] %s" % path)
    common.run_file(path)

    result = dict(
        full_samples=[dict(id=c["id"], code=c["code"], capacity=c["sel"],
                           selected=c["sel"]) for c in full_samples],
        over_capacity=dict(id=over_sample["id"], code=over_sample["code"],
                           capacity=over_sample["sel"] - 3, selected=over_sample["sel"]),
        no_schedule=[dict(id=c["id"], code=c["code"]) for c in no_sched_samples],
        schedule_conflict=dict(class_a=dict(id=conf_a["id"], code=conf_a["code"], teacher=conf_a["teacher"]),
                               class_b=dict(id=conf_b["id"], code=conf_b["code"], teacher=conf_b["teacher"]),
                               classroom_id=room_id, weekday=6))
    with open(os.path.join(common.GEN_DIR, "task4_samples.json"), "w", encoding="utf-8") as fh:
        json.dump(result, fh, ensure_ascii=False, indent=2)
    print("[samples] " + json.dumps(result, ensure_ascii=False))

    # ---------- 校验 ----------
    print("[check] sem5 满班(sel=cap)班数:",
          common.query_ints("SELECT COUNT(*) FROM teaching_class tc WHERE semester_id=5 "
                            "AND selected_count=capacity;")[0][0])
    print("[check] sem5 超容量(sel>cap)班数:",
          common.query_ints("SELECT COUNT(*) FROM teaching_class tc WHERE semester_id=5 "
                            "AND selected_count>capacity;")[0][0])
    print("[check] sem5 无排课班数:",
          common.query_ints("SELECT COUNT(*) FROM teaching_class tc WHERE semester_id=5 "
                            "AND NOT EXISTS(SELECT 1 FROM teaching_class_schedule s "
                            "WHERE s.teaching_class_id=tc.id);")[0][0])


if __name__ == "__main__":
    main()
