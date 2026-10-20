-- 快照刷新完成后，对 stat_* 依赖项做最终复算（只读）
USE edu_course_selection;

SELECT '=== 最终行数 ===' AS section;
SELECT 'stat_credit_progress' t, COUNT(*) c FROM stat_credit_progress
UNION ALL SELECT 'stat_semester_overview', COUNT(*) FROM stat_semester_overview
UNION ALL SELECT 'stat_course_selection', COUNT(*) FROM stat_course_selection
UNION ALL SELECT 'stat_teacher_workload', COUNT(*) FROM stat_teacher_workload
UNION ALL SELECT 'stat_classroom_usage', COUNT(*) FROM stat_classroom_usage;

SELECT '=== 3. S-01 distinct progress_rate（min 应 >1） ===' AS section;
SELECT MIN(cnt) AS min_distinct, MAX(cnt) AS max_distinct, SUM(cnt=1) AS students_with_1
FROM (SELECT student_id, COUNT(DISTINCT progress_rate) cnt FROM stat_credit_progress GROUP BY student_id) t;

SELECT '--- 3.1 分布 ---' AS note;
SELECT cnt_distinct, COUNT(*) students FROM (
  SELECT student_id, COUNT(DISTINCT progress_rate) cnt_distinct
  FROM stat_credit_progress GROUP BY student_id) t
GROUP BY cnt_distinct ORDER BY cnt_distinct;

SELECT '--- 3.2 两个不同年级学生的 9 学期 completed_credit（应递增） ---' AS note2;
SELECT p.student_id, gc.grade_name, s.semester_code, p.completed_credit, p.progress_rate, p.risk_level
FROM stat_credit_progress p
JOIN semester s ON s.id=p.semester_id
JOIN student st ON st.id=p.student_id
JOIN grade_cohort gc ON gc.id=st.grade_id
WHERE p.student_id IN (700, 1500)
ORDER BY p.student_id, s.start_date;

SELECT '--- 3.3 每学期水位（应递增） ---' AS note3;
SELECT s.semester_code, ROUND(AVG(p.completed_credit),2) avg_completed,
       ROUND(AVG(p.progress_rate),2) avg_progress
FROM stat_credit_progress p JOIN semester s ON s.id=p.semester_id
GROUP BY s.semester_code, s.start_date ORDER BY s.start_date;

SELECT '--- 3.4 首学期值 vs 最终值（首学期不应等于最终累计） ---' AS note4;
SELECT (SELECT ROUND(AVG(completed_credit),2) FROM stat_credit_progress p JOIN semester s ON s.id=p.semester_id
        WHERE s.semester_code='2022-2023-1') AS first_sem_avg,
       (SELECT ROUND(AVG(completed_credit),2) FROM stat_credit_progress WHERE semester_id=5) AS latest_avg;

SELECT '=== 11. 毕业风险分档（sem5 学生口径） ===' AS section;
SELECT risk_level, COUNT(*) students FROM stat_credit_progress WHERE semester_id=5
GROUP BY risk_level ORDER BY students DESC;

SELECT '--- 11.1 年级 × risk ---' AS note;
SELECT gc.grade_name, COUNT(*) total,
  SUM(p.risk_level='HIGH') high_n, SUM(p.risk_level='MEDIUM') med_n, SUM(p.risk_level='LOW') low_n
FROM stat_credit_progress p JOIN grade_cohort gc ON gc.id=p.grade_id
WHERE p.semester_id=5 GROUP BY gc.grade_name, gc.entry_year ORDER BY gc.entry_year;

SELECT '=== 12. 满班率 / 教室利用率 / 教师工作量 ===' AS section;
SELECT semester_id, COUNT(*) total, SUM(selected_count>=capacity AND capacity>0) full_n,
       ROUND(100*SUM(selected_count>=capacity AND capacity>0)/COUNT(*),2) full_pct
FROM stat_course_selection GROUP BY semester_id ORDER BY semester_id;

SELECT '--- 12.1 教室利用率（max 应 <=100） ---' AS note;
SELECT semester_id, COUNT(*) n, MIN(usage_rate) min_r, MAX(usage_rate) max_r, SUM(usage_rate>100) over_100
FROM stat_classroom_usage GROUP BY semester_id ORDER BY semester_id;

SELECT '--- 12.2 教师工作量梯度 ---' AS note2;
SELECT semester_id, COUNT(*) n, MIN(total_hours) min_h, MAX(total_hours) max_h, SUM(total_hours=0) zero_h
FROM stat_teacher_workload GROUP BY semester_id ORDER BY semester_id;

SELECT '=== 12.3 快照与基础数据一致性（口径核对） ===' AS section;
SELECT COUNT(*) AS scs_mismatch FROM stat_course_selection scs
JOIN (SELECT tc.id, COUNT(cs.id) c FROM teaching_class tc
      LEFT JOIN course_selection cs ON cs.teaching_class_id=tc.id AND cs.status='SELECTED'
      GROUP BY tc.id) b ON b.id=scs.teaching_class_id
WHERE scs.selected_count <> b.c;

SELECT '--- 12.4 overview 三口径 vs 实际 ---' AS note;
SELECT o.semester_id, o.selection_count o_sel, b.sel real_sel, o.withdraw_count o_wdr, b.wdr real_wdr,
       o.waitlist_count o_wai, b.wai real_wai
FROM stat_semester_overview o
JOIN (SELECT tc.semester_id sid, SUM(cs.status='SELECTED') sel, SUM(cs.status='WITHDRAWN') wdr,
             SUM(cs.status='WAITING') wai
      FROM course_selection cs JOIN teaching_class tc ON tc.id=cs.teaching_class_id
      GROUP BY tc.semester_id) b ON b.sid=o.semester_id
ORDER BY o.semester_id;

SELECT '=== 13. withdraw_count 全部非 0 ===' AS section;
SELECT semester_id, withdraw_count FROM stat_semester_overview ORDER BY semester_id;
SELECT COUNT(*) AS zero_withdraw_semesters FROM stat_semester_overview WHERE withdraw_count=0;

SELECT '=== 14. 教师 T10001 / T10002 快照 ===' AS section;
SELECT teacher_id, teacher_no, teacher_name, class_count, total_hours, student_count
FROM stat_teacher_workload WHERE semester_id=5 AND teacher_id IN (1,2);

SELECT '=== 15. 退课率复核（口径：退课/(选课+退课)） ===' AS section;
SELECT tc.semester_id, s.semester_code,
  SUM(cs.status='SELECTED') sel, SUM(cs.status='WITHDRAWN') wdr,
  ROUND(100*SUM(cs.status='WITHDRAWN')/NULLIF(SUM(cs.status IN ('SELECTED','WITHDRAWN')),0),2) wdr_rate_pct
FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN semester s ON s.id=tc.semester_id
GROUP BY tc.semester_id, s.semester_code, s.start_date ORDER BY s.start_date;
