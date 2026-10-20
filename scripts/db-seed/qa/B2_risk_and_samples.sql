-- B12 修正口径：按"学生"（取当前学期 sem5 快照）统计毕业风险；并核对样本教学班
USE edu_course_selection;

SELECT '=== B12-R 全表 18000 行 vs 2000 学生：快照是按学期重复的 ===' AS section;
SELECT COUNT(*) rows_all, COUNT(DISTINCT student_id) distinct_student,
       COUNT(DISTINCT semester_id) distinct_sem, COUNT(*)/COUNT(DISTINCT student_id) rows_per_student
FROM stat_credit_progress;

SELECT '--- B12-R1 每个学期快照的风险分布（行数口径） ---' AS note;
SELECT semester_id, COUNT(*) n,
  SUM(risk_level='HIGH') h, SUM(risk_level='MEDIUM') m, SUM(risk_level='LOW') l,
  ROUND(100*SUM(risk_level='HIGH')/COUNT(*),2) high_pct
FROM stat_credit_progress GROUP BY semester_id ORDER BY semester_id;

SELECT '--- B12-R2 sem5 快照：年级 × risk_level（唯一学生口径） ---' AS note;
SELECT gc.grade_name, gc.entry_year, COUNT(*) AS students,
  SUM(p.risk_level='HIGH') AS high_cnt, SUM(p.risk_level='MEDIUM') AS medium_cnt, SUM(p.risk_level='LOW') AS low_cnt,
  ROUND(100*SUM(p.risk_level='HIGH')/COUNT(*),2) AS high_pct,
  ROUND(AVG(p.progress_rate),2) AS avg_progress
FROM stat_credit_progress p JOIN grade_cohort gc ON gc.id=p.grade_id
WHERE p.semester_id=5
GROUP BY gc.grade_name, gc.entry_year ORDER BY gc.entry_year;

SELECT '--- B12-R3 sem5 风险总分布（学生口径） ---' AS note;
SELECT risk_level, COUNT(*) students, ROUND(100*COUNT(*)/(SELECT COUNT(*) FROM stat_credit_progress WHERE semester_id=5),2) pct
FROM stat_credit_progress WHERE semester_id=5 GROUP BY risk_level ORDER BY students DESC;

SELECT '--- B12-R4 sem5 progress_rate 分桶（学生口径） ---' AS note;
SELECT CASE WHEN progress_rate<30 THEN 'a.<30' WHEN progress_rate<60 THEN 'b.30-60'
            WHEN progress_rate<80 THEN 'c.60-80' WHEN progress_rate<=100 THEN 'd.80-100' ELSE 'e.>100' END bucket,
       COUNT(*) n FROM stat_credit_progress WHERE semester_id=5 GROUP BY bucket ORDER BY bucket;

SELECT '=== S 异常样本教学班复核 ===' AS section;
SELECT tc.id, tc.class_code, tc.semester_id, tc.capacity, tc.selected_count,
       c.course_code, c.course_name
FROM teaching_class tc JOIN course c ON c.id=tc.course_id
WHERE tc.id IN (739,759,373,817,857,974,737,800,935,738,740) ORDER BY tc.id;

SELECT '--- S1 成绩统计（739/759/373/817） ---' AS note;
SELECT tc.id, COUNT(sc.id) score_rows,
       ROUND(AVG(sc.score_value),2) avg_score, ROUND(STDDEV_POP(sc.score_value),2) sd,
       ROUND(100*SUM(sc.score_value>=60)/NULLIF(COUNT(sc.id),0),2) pass_pct,
       ROUND(100*SUM(sc.score_value>=85)/NULLIF(COUNT(sc.id),0),2) exc_pct,
       tc.selected_count
FROM teaching_class tc
LEFT JOIN course_selection cs ON cs.teaching_class_id=tc.id AND cs.status='SELECTED'
LEFT JOIN score sc ON sc.course_selection_id=cs.id
WHERE tc.id IN (739,759,373,817,857,974,737)
GROUP BY tc.id, tc.selected_count ORDER BY tc.id;

SELECT '--- S2 无排课的班（800/935）schedule 行数 ---' AS note;
SELECT tc.id, COUNT(s.id) sched_rows FROM teaching_class tc
LEFT JOIN teaching_class_schedule s ON s.teaching_class_id=tc.id
WHERE tc.id IN (800,935) GROUP BY tc.id;

SELECT '--- S3 冲突班 738/740 的排课 ---' AS note;
SELECT s.teaching_class_id, s.classroom_id, s.weekday, s.start_section, s.end_section,
       s.start_week, s.end_week, tc.teacher_id, tc.semester_id
FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
WHERE s.teaching_class_id IN (738,740) ORDER BY s.classroom_id, s.weekday, s.start_section;

SELECT '=== B16-V 巡检冲突真实性回溯 ===' AS section;
SELECT '--- 教室冲突：同一教室+weekday+节次 在 sem5 有 >1 个班的组合（真实冲突组） ---' AS note;
SELECT s.classroom_id, s.weekday, s.start_section,
       COUNT(DISTINCT s.teaching_class_id) class_cnt,
       GROUP_CONCAT(DISTINCT s.teaching_class_id ORDER BY s.teaching_class_id) class_ids
FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
WHERE tc.semester_id=5
GROUP BY s.classroom_id, s.weekday, s.start_section
HAVING class_cnt>1
ORDER BY class_cnt DESC, s.classroom_id LIMIT 15;

SELECT '--- 真实冲突组总数 与 巡检 CLASSROOM 条数对比 ---' AS note;
SELECT (SELECT COUNT(*) FROM (
   SELECT s.classroom_id FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
   WHERE tc.semester_id=5 GROUP BY s.classroom_id, s.weekday, s.start_section HAVING COUNT(DISTINCT s.teaching_class_id)>1) a) AS true_classroom_conflict_groups,
 (SELECT COUNT(DISTINCT biz_id) FROM data_audit_issue WHERE biz_type='CLASSROOM') AS audit_classroom_cnt,
 (SELECT COUNT(*) FROM (
   SELECT tc.teacher_id FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
   WHERE tc.semester_id=5 GROUP BY tc.teacher_id, s.weekday, s.start_section HAVING COUNT(DISTINCT s.teaching_class_id)>1) b) AS true_teacher_conflict_groups,
 (SELECT COUNT(DISTINCT biz_id) FROM data_audit_issue WHERE biz_type='TEACHER') AS audit_teacher_cnt;

SELECT '--- 教师冲突样例：classroom 1 / weekday 6 / 第1-2节 ---' AS note;
SELECT s.teaching_class_id, s.classroom_id, s.weekday, s.start_section, s.end_section, tc.teacher_id
FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
WHERE tc.semester_id=5 AND s.weekday=6 AND s.start_section=1 ORDER BY s.classroom_id, s.teaching_class_id LIMIT 20;

SELECT '=== X 教师覆盖度 ===' AS section;
SELECT '总教师数' item, COUNT(*) v FROM teacher
UNION ALL SELECT '有教学班的教师数', COUNT(DISTINCT teacher_id) FROM teaching_class WHERE teacher_id IS NOT NULL
UNION ALL SELECT '任一学期出现在 workload 快照的教师数', COUNT(DISTINCT teacher_id) FROM stat_teacher_workload;

SELECT '--- X1 从未授课教师（前 10） ---' AS note;
SELECT t.id, t.teacher_no, t.teacher_name, t.title FROM teacher t
WHERE NOT EXISTS (SELECT 1 FROM teaching_class tc WHERE tc.teacher_id=t.id) LIMIT 10;
