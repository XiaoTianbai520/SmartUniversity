-- 第3轮定点复验：A1/A2/A3 + C(结构/时间越界)（只读）
USE edu_course_selection;

SELECT '=== 采样时间 ===' AS section;
SELECT NOW() AS sampled_at;

SELECT '=== A1: 教学班 1014 成绩行数（应 0） ===' AS section;
SELECT COUNT(*) AS tc1014_scores FROM score s
JOIN course_selection cs ON cs.id=s.course_selection_id
WHERE cs.teaching_class_id=1014;
SELECT '--- A1.2 三条脏成绩是否还在（应空） ---' AS note;
SELECT id, score_value, status FROM score WHERE id IN (156211,156212,156213);

SELECT '=== A2: 2026级入学前选课 ===' AS section;
SELECT '2026级 SELECTED 的学期分布（应仅 semester_id=5）' AS item;
SELECT tc.semester_id, COUNT(*) c FROM course_selection cs
JOIN student st ON st.id=cs.student_id
JOIN grade_cohort g ON g.id=st.grade_id
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
WHERE cs.status='SELECTED' AND g.entry_year=2026
GROUP BY tc.semester_id ORDER BY tc.semester_id;

SELECT '--- A2.1 2026级各状态分布（申称 2647/69/412） ---' AS note;
SELECT cs.status, COUNT(*) c FROM course_selection cs
JOIN student st ON st.id=cs.student_id
JOIN grade_cohort g ON g.id=st.grade_id
WHERE g.entry_year=2026 GROUP BY cs.status ORDER BY cs.status;

SELECT '--- A2.2 入学前违规：entry_year > YEAR(开学年) 的 SELECTED（应 0） ---' AS note2;
SELECT COUNT(*) AS pre_entry_violation FROM course_selection cs
JOIN student st ON st.id=cs.student_id
JOIN grade_cohort g ON g.id=st.grade_id
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN semester s ON s.id=tc.semester_id
WHERE cs.status='SELECTED' AND g.entry_year > YEAR(s.start_date);

SELECT '--- A2.3 sem4 基线（应 SELECTED 8876 / 成绩 8874） ---' AS note3;
SELECT COUNT(DISTINCT cs.id) AS sem4_selected, COUNT(sc.id) AS sem4_scored
FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
LEFT JOIN score sc ON sc.course_selection_id=cs.id
WHERE tc.semester_id=4 AND cs.status='SELECTED';
SELECT '--- A2.4 sem4 各状态行数 ---' AS note4;
SELECT cs.status, COUNT(*) c FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
WHERE tc.semester_id=4 GROUP BY cs.status;

SELECT '=== A3: sem3 / sem4 满班班数（应各 3） ===' AS section;
SELECT semester_id, SUM(selected_count>=capacity AND capacity>0) AS full_n, COUNT(*) total
FROM teaching_class WHERE semester_id IN (3,4) GROUP BY semester_id;

SELECT '--- A3.1 满班明细 ---' AS note;
SELECT id, semester_id, class_code, capacity, selected_count,
       (selected_count=capacity) AS exact_full, (selected_count>capacity) AS over_cap
FROM teaching_class
WHERE semester_id IN (3,4) AND selected_count>=capacity AND capacity>0
ORDER BY semester_id, id;

SELECT '--- A3.2 是否产生 CAPACITY_ZERO / over_capacity ---' AS note2;
SELECT semester_id, SUM(capacity=0) cap_zero, SUM(selected_count>capacity) over_cap,
       SUM(selected_count=capacity) exact_full
FROM teaching_class WHERE semester_id IN (3,4) GROUP BY semester_id;

SELECT '--- A3.3 sem3 班级规模分布（验证两极化说法） ---' AS note3;
SELECT CASE WHEN capacity<30 THEN 'a.<30' WHEN capacity<60 THEN 'b.30-59'
            WHEN capacity<100 THEN 'c.60-99' ELSE 'd.>=100' END band,
       COUNT(*) c, MIN(capacity) mn, MAX(capacity) mx
FROM teaching_class WHERE semester_id=3 GROUP BY band ORDER BY band;

SELECT '=== C: stat_* 结构检查 ===' AS section;
SELECT s.semester_code, s.id AS sid,
  (SELECT COUNT(*) FROM stat_credit_progress p WHERE p.semester_id=s.id) AS credit_rows,
  (SELECT COUNT(DISTINCT p.student_id) FROM stat_credit_progress p WHERE p.semester_id=s.id) AS credit_distinct,
  (SELECT COUNT(*) FROM stat_classroom_usage c WHERE c.semester_id=s.id) AS room_rows,
  (SELECT COUNT(*) FROM stat_course_selection x WHERE x.semester_id=s.id) AS scs_rows,
  (SELECT COUNT(*) FROM teaching_class t WHERE t.semester_id=s.id) AS tc_rows,
  (SELECT COUNT(*) FROM stat_teacher_workload w WHERE w.semester_id=s.id) AS wl_rows,
  (SELECT COUNT(*) FROM stat_semester_overview o WHERE o.semester_id=s.id) AS ov_rows
FROM semester s ORDER BY s.start_date;

SELECT '--- C.1 重复分组（应全空） ---' AS note;
SELECT 'credit' k, COUNT(*) dup FROM (SELECT semester_id, student_id FROM stat_credit_progress
  GROUP BY semester_id, student_id HAVING COUNT(*)>1) a
UNION ALL SELECT 'room', COUNT(*) FROM (SELECT semester_id, classroom_id FROM stat_classroom_usage
  GROUP BY semester_id, classroom_id HAVING COUNT(*)>1) b
UNION ALL SELECT 'scs', COUNT(*) FROM (SELECT semester_id, teaching_class_id FROM stat_course_selection
  GROUP BY semester_id, teaching_class_id HAVING COUNT(*)>1) c
UNION ALL SELECT 'workload', COUNT(*) FROM (SELECT semester_id, teacher_id FROM stat_teacher_workload
  GROUP BY semester_id, teacher_id HAVING COUNT(*)>1) d;

SELECT '--- C.2 stat_date 是否单一当日 ---' AS note2;
SELECT 'stat_credit_progress' t, GROUP_CONCAT(DISTINCT stat_date) dates FROM stat_credit_progress
UNION ALL SELECT 'stat_semester_overview', GROUP_CONCAT(DISTINCT stat_date) FROM stat_semester_overview
UNION ALL SELECT 'stat_course_selection', GROUP_CONCAT(DISTINCT stat_date) FROM stat_course_selection
UNION ALL SELECT 'stat_teacher_workload', GROUP_CONCAT(DISTINCT stat_date) FROM stat_teacher_workload
UNION ALL SELECT 'stat_classroom_usage', GROUP_CONCAT(DISTINCT stat_date) FROM stat_classroom_usage;

SELECT '=== C: 四个时间越界（应全 0） ===' AS section;
SELECT
  SUM(cs.selected_at > b.drop_deadline) AS sel_after_dl,
  SUM(cs.selected_at < b.start_time)    AS sel_before_start,
  SUM(cs.status='WITHDRAWN' AND cs.withdrawn_at > b.drop_deadline) AS wdr_after_dl,
  SUM(cs.status='WITHDRAWN' AND cs.withdrawn_at < cs.selected_at)  AS wdr_before_sel,
  COUNT(*) AS total
FROM course_selection cs JOIN selection_batch b ON b.id=cs.batch_id;

SELECT '=== 全库行数 ===' AS section;
SELECT 'course_selection' t, COUNT(*) c FROM course_selection
UNION ALL SELECT 'score', COUNT(*) FROM score
UNION ALL SELECT 'teaching_class', COUNT(*) FROM teaching_class
UNION ALL SELECT 'semester_archive', COUNT(*) FROM semester_archive;

SELECT '=== 巡检分组（应 1/2/51/44 = 98） ===' AS section;
SELECT rule_code, biz_type, COUNT(*) c FROM data_audit_issue GROUP BY rule_code, biz_type
ORDER BY rule_code, biz_type;
SELECT stat_date, COUNT(*) c FROM data_audit_issue GROUP BY stat_date;
