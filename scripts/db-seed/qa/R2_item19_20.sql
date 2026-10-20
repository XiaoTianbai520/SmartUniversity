-- QA-2 补充：第 19/20 项 + 退课时间复核（只读，每条带采样时间）
USE edu_course_selection;

SELECT '=== 采样时间 ===' AS section;
SELECT NOW() AS sampled_at, CURDATE() AS today;

SELECT '=== 19. 覆盖写无重复/无残留：每学期行数 ===' AS section;
SELECT s.semester_code, s.id AS semester_id,
       (SELECT COUNT(*) FROM stat_credit_progress p WHERE p.semester_id=s.id) AS credit_rows,
       (SELECT COUNT(DISTINCT p.student_id) FROM stat_credit_progress p WHERE p.semester_id=s.id) AS credit_distinct_stu,
       (SELECT COUNT(*) FROM stat_classroom_usage c WHERE c.semester_id=s.id) AS classroom_rows,
       (SELECT COUNT(DISTINCT c.classroom_id) FROM stat_classroom_usage c WHERE c.semester_id=s.id) AS classroom_distinct_room,
       (SELECT COUNT(*) FROM stat_course_selection x WHERE x.semester_id=s.id) AS scs_rows,
       (SELECT COUNT(*) FROM teaching_class t WHERE t.semester_id=s.id) AS tc_rows,
       (SELECT COUNT(*) FROM stat_teacher_workload w WHERE w.semester_id=s.id) AS workload_rows,
       (SELECT COUNT(*) FROM stat_semester_overview o WHERE o.semester_id=s.id) AS overview_rows,
       (SELECT COUNT(*) FROM classroom) AS classroom_total,
       (SELECT COUNT(*) FROM student) AS student_total
FROM semester s ORDER BY s.start_date;

SELECT '--- 19.1 逐学期逐学生是否重复（应为空） ---' AS note;
SELECT semester_id, COUNT(*) dup_groups FROM (
  SELECT semester_id, student_id FROM stat_credit_progress GROUP BY semester_id, student_id HAVING COUNT(*)>1) t
GROUP BY semester_id;

SELECT '--- 19.2 逐学期逐教室是否重复（应为空） ---' AS note2;
SELECT semester_id, COUNT(*) dup_groups FROM (
  SELECT semester_id, classroom_id FROM stat_classroom_usage GROUP BY semester_id, classroom_id HAVING COUNT(*)>1) t
GROUP BY semester_id;

SELECT '--- 19.3 逐学期逐教学班是否重复（应为空） ---' AS note3;
SELECT semester_id, COUNT(*) dup_groups FROM (
  SELECT semester_id, teaching_class_id FROM stat_course_selection GROUP BY semester_id, teaching_class_id HAVING COUNT(*)>1) t
GROUP BY semester_id;

SELECT '--- 19.4 是否存在跨日期残留（stat_date 去重） ---' AS note4;
SELECT 'stat_credit_progress' t, GROUP_CONCAT(DISTINCT stat_date) dates FROM stat_credit_progress
UNION ALL SELECT 'stat_semester_overview', GROUP_CONCAT(DISTINCT stat_date) FROM stat_semester_overview
UNION ALL SELECT 'stat_course_selection', GROUP_CONCAT(DISTINCT stat_date) FROM stat_course_selection
UNION ALL SELECT 'stat_teacher_workload', GROUP_CONCAT(DISTINCT stat_date) FROM stat_teacher_workload
UNION ALL SELECT 'stat_classroom_usage', GROUP_CONCAT(DISTINCT stat_date) FROM stat_classroom_usage;

SELECT '=== 20. selected_at 是否落在批次窗口内（全状态口径） ===' AS section;
SELECT COUNT(*) AS total_with_batch,
       SUM(cs.selected_at > b.drop_deadline) AS after_deadline,
       SUM(cs.selected_at < b.start_time)    AS before_start,
       SUM(cs.selected_at IS NULL)           AS null_selected_at
FROM course_selection cs JOIN selection_batch b ON b.id=cs.batch_id;

SELECT '--- 20.1 越界按学期/批次分布 ---' AS note;
SELECT tc.semester_id, cs.batch_id, b.start_time, b.drop_deadline, cs.status,
       COUNT(*) AS rows_cnt,
       SUM(cs.selected_at > b.drop_deadline) AS after_dl,
       SUM(cs.selected_at < b.start_time)    AS before_start
FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN selection_batch b ON b.id=cs.batch_id
GROUP BY tc.semester_id, cs.batch_id, b.start_time, b.drop_deadline, cs.status
ORDER BY after_dl DESC, tc.semester_id LIMIT 20;

SELECT '--- 20.2 越界样例 ---' AS note2;
SELECT cs.id, cs.student_id, cs.status, cs.selected_at, b.start_time, b.drop_deadline, tc.semester_id
FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN selection_batch b ON b.id=cs.batch_id
WHERE cs.selected_at > b.drop_deadline OR cs.selected_at < b.start_time
ORDER BY cs.id LIMIT 5;

SELECT '=== 7. 退课时间两项复核（目标 0 / 0） ===' AS section;
SELECT COUNT(*) AS total_withdrawn,
       SUM(cs.withdrawn_at > b.drop_deadline) AS after_deadline,
       SUM(cs.withdrawn_at < cs.selected_at)  AS before_selected,
       SUM(cs.withdrawn_at IS NULL)           AS null_withdrawn
FROM course_selection cs
JOIN selection_batch b ON b.id=cs.batch_id
WHERE cs.status='WITHDRAWN';

SELECT '--- 7.1 同一时刻退课（selected_at = withdrawn_at）的条数 ---' AS note;
SELECT COUNT(*) AS same_instant FROM course_selection
WHERE status='WITHDRAWN' AND selected_at = withdrawn_at;

SELECT '--- 7.2 距 drop_deadline 1 天内退课的条数（疑似特殊种子） ---' AS note2;
SELECT COUNT(*) AS within_1day FROM course_selection cs JOIN selection_batch b ON b.id=cs.batch_id
WHERE cs.status='WITHDRAWN' AND cs.withdrawn_at > b.drop_deadline - INTERVAL 1 DAY;
