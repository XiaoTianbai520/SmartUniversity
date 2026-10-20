# -*- coding: utf-8 -*-
"""任务#2（第一部分）：以 2026-2027-1 的 370 个教学班为模板，
为 6 个空学期克隆教学班 + 专业/年级适用范围 + 排课 + 批次关联。

幂等：每次运行先删除目标学期的教学班及其选课/成绩/批次关联，再重建。

输出：scripts/db-seed/_gen/03_classes.sql
"""
import hashlib
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common

TEMPLATE_SEM_CODE = "2026-2027-1"
TARGET_CODES = [
    "2022-2023-1", "2022-2023-2", "2023-2024-1",
    "2023-2024-2", "2024-2025-1", "2024-2025-2",
]


def stable_unit(*parts):
    """返回 [0,1) 稳定伪随机数（同一输入跨进程一致）。"""
    raw = "|".join(str(p) for p in parts).encode("utf-8")
    digest = hashlib.md5(raw).hexdigest()
    return int(digest[:12], 16) / float(16 ** 12)


def main():
    # ---------- 1. 读取学期 ----------
    sem_rows = common.query(
        "SELECT id,semester_code FROM semester WHERE semester_code IN (%s);"
        % ",".join(common.sql_escape(c) for c in TARGET_CODES + [TEMPLATE_SEM_CODE])
    )
    sem_id = {r[1]: int(r[0]) for r in sem_rows}
    missing = [c for c in TARGET_CODES + [TEMPLATE_SEM_CODE] if c not in sem_id]
    if missing:
        raise SystemExit("缺少学期: %s" % missing)
    tpl_sem = sem_id[TEMPLATE_SEM_CODE]
    target_ids = [sem_id[c] for c in TARGET_CODES]

    # ---------- 2. 读取模板（sem5 教学班） ----------
    tpl = common.query(
        "SELECT tc.id, tc.class_code, tc.class_name, tc.course_id, tc.teacher_id, "
        "       tc.capacity, c.course_code, c.course_type, c.credit "
        "FROM teaching_class tc JOIN course c ON c.id = tc.course_id "
        "WHERE tc.semester_id = %d ORDER BY tc.id;" % tpl_sem
    )
    if not tpl:
        raise SystemExit("模板学期无教学班")
    # 每门课的班级序号：沿用模板 class_code 末尾的两位序号
    tpl_classes = []
    for r in tpl:
        tc_id, class_code, class_name, course_id, teacher_id = (
            int(r[0]), r[1], r[2], int(r[3]), int(r[4]))
        capacity = int(r[5])
        course_code, course_type, credit = r[6], r[7], float(r[8])
        suffix = class_code.rsplit("-", 1)[-1]
        tpl_classes.append(dict(
            id=tc_id, suffix=suffix, class_name=class_name, course_id=course_id,
            teacher_id=teacher_id, capacity=capacity, course_code=course_code,
            course_type=course_type, credit=credit))

    # ---------- 3. 读取模板适用范围 ----------
    tpl_ids = [c["id"] for c in tpl_classes]
    tpl_id_csv = ",".join(str(i) for i in tpl_ids)
    m_rows = common.query(
        "SELECT teaching_class_id, major_id FROM teaching_class_major "
        "WHERE teaching_class_id IN (%s);" % tpl_id_csv)
    tcm = {}
    for r in m_rows:
        tcm.setdefault(int(r[0]), []).append(int(r[1]))
    g_rows = common.query(
        "SELECT teaching_class_id, grade_id FROM teaching_class_grade "
        "WHERE teaching_class_id IN (%s);" % tpl_id_csv)
    tcg = {}
    for r in g_rows:
        tcg.setdefault(int(r[0]), []).append(int(r[1]))
    s_rows = common.query(
        "SELECT teaching_class_id, classroom_id, weekday, start_section, end_section "
        "FROM teaching_class_schedule WHERE teaching_class_id IN (%s) "
        "ORDER BY teaching_class_id, id;" % tpl_id_csv)
    sched = {}
    for r in s_rows:
        sched.setdefault(int(r[0]), []).append(
            (int(r[1]), int(r[2]), int(r[3]), int(r[4])))

    # ---------- 4. 热门课程：容量压低，制造「选课人数 > 容量」梯度 ----------
    pop = {c["course_id"]: stable_unit("pop", c["course_id"]) for c in tpl_classes}
    hot_courses = set(
        sorted(pop, key=lambda cid: -pop[cid])[:10])   # 最热 10 门
    cap_scale = {}
    for c in tpl_classes:
        if c["course_id"] in hot_courses:
            cap_scale[c["course_id"]] = 0.45
        else:
            cap_scale[c["course_id"]] = 1.0

    # ---------- 5. 幂等重置目标学期 ----------
    id_list = ",".join(str(i) for i in target_ids)
    reset_sql = [
        "DELETE sc FROM score sc JOIN course_selection cs ON cs.id = sc.course_selection_id "
        "JOIN teaching_class tc ON tc.id = cs.teaching_class_id "
        "WHERE tc.semester_id IN (%s);" % id_list,
        "DELETE cs FROM course_selection cs JOIN teaching_class tc "
        "ON tc.id = cs.teaching_class_id WHERE tc.semester_id IN (%s);" % id_list,
        "DELETE sbc FROM selection_batch_class sbc JOIN teaching_class tc "
        "ON tc.id = sbc.teaching_class_id WHERE tc.semester_id IN (%s);" % id_list,
        "DELETE FROM teaching_class WHERE semester_id IN (%s);" % id_list,
    ]
    common.run_sql("\n".join(reset_sql))
    print("[reset] 已清理目标学期旧教学班")

    max_id = common.query_ints("SELECT COALESCE(MAX(id),0) FROM teaching_class;")[0][0]
    next_id = max_id + 1

    # ---------- 6. 批次 ----------
    batch_rows = common.query(
        "SELECT semester_id, id FROM selection_batch WHERE semester_id IN (%s) "
        "ORDER BY semester_id;" % id_list)
    batch_of = {}
    for r in batch_rows:
        batch_of.setdefault(int(r[0]), int(r[1]))

    # ---------- 7. 生成 ----------
    tc_rows, tcm_rows, tcg_rows, sched_rows, sbc_rows = [], [], [], [], []
    new_ids_by_sem = {}
    for sem_idx, code in enumerate(TARGET_CODES):
        sid = sem_id[code]
        new_ids_by_sem[sid] = []
        batch_id = batch_of.get(sid)
        if batch_id is None:
            raise SystemExit("学期 %s 缺少选课批次" % code)
        for c in tpl_classes:
            new_id = next_id
            next_id += 1
            new_ids_by_sem[sid].append(new_id)
            new_code = "%s-%s-%s" % (code, c["course_code"], c["suffix"])
            cap = max(15, int(round(c["capacity"] * cap_scale[c["course_id"]])))
            tc_rows.append((
                new_id, new_code, c["class_name"], c["course_id"], sid,
                c["teacher_id"], cap, 0, "CLOSED"))
            for mid in tcm.get(c["id"], [1]):
                tcm_rows.append((new_id, mid))
            for gid in tcg.get(c["id"], [1, 2, 3, 4, 5]):
                tcg_rows.append((new_id, gid))
            for (room, wd, ss, es) in sched.get(c["id"], []):
                sched_rows.append((new_id, room, wd, ss, es, 1, 16))
            sbc_rows.append((batch_id, new_id))

    cols_tc = ["id", "class_code", "class_name", "course_id", "semester_id",
               "teacher_id", "capacity", "selected_count", "status"]
    cols_tcm = ["teaching_class_id", "major_id"]
    cols_tcg = ["teaching_class_id", "grade_id"]
    cols_sched = ["teaching_class_id", "classroom_id", "weekday",
                  "start_section", "end_section", "start_week", "end_week"]
    cols_sbc = ["batch_id", "teaching_class_id"]

    stmts = []
    stmts += common.batch_insert("teaching_class", cols_tc, tc_rows, 500)
    stmts += common.batch_insert("teaching_class_major", cols_tcm, tcm_rows, 500)
    stmts += common.batch_insert("teaching_class_grade", cols_tcg, tcg_rows, 500)
    stmts += common.batch_insert("teaching_class_schedule", cols_sched, sched_rows, 500)
    stmts += common.batch_insert("selection_batch_class", cols_sbc, sbc_rows, 500)

    path = common.write_sql("03_classes.sql", stmts,
                            header="克隆教学班/范围/排课/批次关联（task#2 part1）")
    print("[write] %s" % path)
    common.run_file(path)

    # ---------- 8. 校验 ----------
    check = common.query_ints(
        "SELECT semester_id, COUNT(*) FROM teaching_class WHERE semester_id IN (%s) "
        "GROUP BY semester_id ORDER BY semester_id;" % id_list)
    print("[check] 各目标学期教学班数:", check)
    print("[check] teaching_class +%d, tcm +%d, tcg +%d, schedule +%d, sbc +%d"
          % (len(tc_rows), len(tcm_rows), len(tcg_rows), len(sched_rows), len(sbc_rows)))
    print("[check] 热门课程(容量压至45%%): %s" % sorted(hot_courses))


if __name__ == "__main__":
    main()
