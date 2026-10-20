-- QA-2 复验：数据一致性 / 退课补齐 / 分布（只读）
USE edu_course_selection;

SELECT '=== 0. 当前行数基线 ===' AS section;
SELECT 'semester' t, COUNT(*) c FROM semester
UNION ALL SELECT 'teaching_class', COUNT(*) FROM teaching_class
UNION ALL SELECT 'course_selection', COUNT(*) FROM course_selection
UNION ALL SELECT 'score', COUNT(*) FROM score
UNION ALL SELECT 'data_audit_issue', COUNT(*) FROM data_audit_issue
UNION ALL SELECT 'stat_credit_progress', COUNT(*) FROM stat_credit_progress
UNION ALL SELECT 'semester_archive', COUNT(*) FROM semester_archive
UNION ALL SELECT 'stat_semester_overview', COUNT(*) FROM stat_semester_overview
UNION ALL SELECT 'stat_course_selection', COUNT(*) FROM stat_course_selection
UNION ALL SELECT 'stat_teacher_workload', COUNT(*) FROM stat_teacher_workload
UNION ALL SELECT 'stat_classroom_usage', COUNT(*) FROM stat_classroom_usage;

SELECT '=== 4. M-03 唯一键现状 ===' AS section;
SHOW INDEX FROM data_audit_issue;

SELECT '--- 4.1 巡检按 rule_code + biz_type 分组 ---' AS note;
SELECT rule_code, biz_type, COUNT(*) cnt, COUNT(DISTINCT biz_id) distinct_biz
FROM data_audit_issue GROUP BY rule_code, biz_type ORDER BY rule_code, biz_type;

SELECT '--- 4.2 上轮漏报的教室是否都有记录 ---' AS note;
SELECT 'CLASSROOM' AS biz_type, id AS should_exist,
       EXISTS(SELECT 1 FROM data_audit_issue d WHERE d.biz_type='CLASSROOM' AND d.biz_id=id) AS has_record
FROM (SELECT 41 AS id UNION SELECT 53 UNION SELECT 13 UNION SELECT 59 UNION SELECT 47) t
UNION ALL
SELECT 'TEACHER', id,
       EXISTS(SELECT 1 FROM data_audit_issue d WHERE d.biz_type='TEACHER' AND d.biz_id=id)
FROM (SELECT 3 AS id UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 18
      UNION SELECT 22 UNION SELECT 25 UNION SELECT 27 UNION SELECT 54) t2;

SELECT '--- 4.3 真实冲突覆盖度：教室 / 教师 应各 51 / 44 ---' AS note;
WITH real_cls AS (
  SELECT DISTINCT a.classroom_id AS id FROM teaching_class_schedule a
  JOIN teaching_class_schedule b ON a.classroom_id=b.classroom_id AND a.weekday=b.weekday
    AND a.teaching_class_id<b.teaching_class_id
    AND a.start_section<=b.end_section AND b.start_section<=a.end_section
    AND a.start_week<=b.end_week AND b.start_week<=a.end_week
  JOIN teaching_class ta ON ta.id=a.teaching_class_id JOIN teaching_class tb ON tb.id=b.teaching_class_id
  WHERE ta.semester_id=5 AND tb.semester_id=5),
real_tch AS (
  SELECT DISTINCT ta.teacher_id AS id FROM teaching_class_schedule a
  JOIN teaching_class_schedule b ON a.weekday=b.weekday AND a.teaching_class_id<b.teaching_class_id
    AND a.start_section<=b.end_section AND b.start_section<=a.end_section
    AND a.start_week<=b.end_week AND b.start_week<=a.end_week
  JOIN teaching_class ta ON ta.id=a.teaching_class_id JOIN teaching_class tb ON tb.id=b.teaching_class_id
  WHERE ta.semester_id=5 AND tb.semester_id=5 AND ta.teacher_id=tb.teacher_id)
SELECT (SELECT COUNT(*) FROM real_cls) AS real_classrooms,
       (SELECT COUNT(*) FROM data_audit_issue WHERE biz_type='CLASSROOM') AS audit_classrooms,
       (SELECT COUNT(*) FROM real_tch) AS real_teachers,
       (SELECT COUNT(*) FROM data_audit_issue WHERE biz_type='TEACHER') AS audit_teachers;

SELECT '--- 4.4 漏报明细（应为空） ---' AS note;
WITH real_cls AS (
  SELECT DISTINCT a.classroom_id AS id FROM teaching_class_schedule a
  JOIN teaching_class_schedule b ON a.classroom_id=b.classroom_id AND a.weekday=b.weekday
    AND a.teaching_class_id<b.teaching_class_id
    AND a.start_section<=b.end_section AND b.start_section<=a.end_section
    AND a.start_week<=b.end_week AND b.start_week<=a.end_week
  JOIN teaching_class ta ON ta.id=a.teaching_class_id JOIN teaching_class tb ON tb.id=b.teaching_class_id
  WHERE ta.semester_id=5 AND tb.semester_id=5),
real_tch AS (
  SELECT DISTINCT ta.teacher_id AS id FROM teaching_class_schedule a
  JOIN teaching_class_schedule b ON a.weekday=b.weekday AND a.teaching_class_id<b.teaching_class_id
    AND a.start_section<=b.end_section AND b.start_section<=a.end_section
    AND a.start_week<=b.end_week AND b.start_week<=a.end_week
  JOIN teaching_class ta ON ta.id=a.teaching_class_id JOIN teaching_class tb ON tb.id=b.teaching_class_id
  WHERE ta.semester_id=5 AND tb.semester_id=5 AND ta.teacher_id=tb.teacher_id)
SELECT 'CLASSROOM 漏报' AS kind, GROUP_CONCAT(id) ids FROM real_cls
 WHERE id NOT IN (SELECT biz_id FROM data_audit_issue WHERE biz_type='CLASSROOM')
UNION ALL
SELECT 'TEACHER 漏报', GROUP_CONCAT(id) FROM real_tch
 WHERE id NOT IN (SELECT biz_id FROM data_audit_issue WHERE biz_type='TEACHER');

SELECT '=== 5. selected_count vs 实际 SELECTED ===' AS section;
SELECT COUNT(*) AS bad_rows, MAX(ABS(diff)) AS max_abs_diff FROM (
  SELECT tc.id, tc.selected_count - (SELECT COUNT(*) FROM course_selection cs
          WHERE cs.teaching_class_id=tc.id AND cs.status='SELECTED') AS diff
  FROM teaching_class tc) t WHERE diff <> 0;

SELECT '=== 6. 退课无 score / 孤儿 score ===' AS section;
SELECT cs.status, COUNT(*) score_rows
FROM score sc JOIN course_selection cs ON cs.id=sc.course_selection_id
GROUP BY cs.status;
SELECT COUNT(*) AS orphan_score FROM score sc
WHERE NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.id=sc.course_selection_id);

SELECT '=== 7. 退课率与 withdrawn_at 越界 ===' AS section;
SELECT tc.semester_id, s.semester_code,
  SUM(cs.status='SELECTED') sel,
  SUM(cs.status='WITHDRAWN') wdr,
  ROUND(100*SUM(cs.status='WITHDRAWN')/NULLIF(SUM(cs.status='SELECTED'),0),2) wdr_rate_pct,
  SUM(cs.status='WAITING') wai
FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN semester s ON s.id=tc.semester_id
GROUP BY tc.semester_id, s.semester_code ORDER BY s.start_date;

SELECT '--- 7.1 withdrawn_at 是否落在 [selected_at, batch.drop_deadline] ---' AS note;
SELECT COUNT(*) AS total_withdrawn,
  SUM(withdrawn_at IS NULL) AS null_withdrawn_at,
  SUM(selected_at IS NULL) AS null_selected_at,
  SUM(withdrawn_at < selected_at) AS before_selected,
  SUM(b.drop_deadline IS NOT NULL AND withdrawn_at > b.drop_deadline) AS after_deadline,
  SUM(b.start_time IS NOT NULL AND selected_at < b.start_time) AS sel_before_batch_start
FROM course_selection cs
LEFT JOIN selection_batch b ON b.id = cs.batch_id
WHERE cs.status='WITHDRAWN';

SELECT '--- 7.2 退课记录样例 ---' AS note;
SELECT cs.id, cs.student_id, tc.semester_id, cs.status, cs.selected_at, cs.withdrawn_at,
       b.drop_deadline
FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
LEFT JOIN selection_batch b ON b.id=cs.batch_id
WHERE cs.status='WITHDRAWN' AND tc.semester_id=3 LIMIT 5;

SELECT '=== 8. 学分上限 / 重复 / 范围 ===' AS section;
SELECT COUNT(*) AS over_limit_rows FROM (
  SELECT cs.student_id, tc.semester_id, SUM(c.credit) total_credit, s.max_selection_credit
  FROM course_selection cs
  JOIN teaching_class tc ON tc.id=cs.teaching_class_id
  JOIN course c ON c.id=tc.course_id
  JOIN semester s ON s.id=tc.semester_id
  WHERE cs.status='SELECTED'
  GROUP BY cs.student_id, tc.semester_id, s.max_selection_credit) t
WHERE total_credit > max_selection_credit;

SELECT COUNT(*) AS dup_pairs FROM (
  SELECT student_id, teaching_class_id FROM course_selection
  GROUP BY student_id, teaching_class_id HAVING COUNT(*)>1) t;

SELECT COUNT(*) AS major_violation FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN student st ON st.id=cs.student_id
WHERE cs.status='SELECTED'
  AND NOT EXISTS (SELECT 1 FROM teaching_class_major m WHERE m.teaching_class_id=tc.id AND m.major_id=st.major_id);

SELECT COUNT(*) AS grade_violation FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN student st ON st.id=cs.student_id
WHERE cs.status='SELECTED'
  AND NOT EXISTS (SELECT 1 FROM teaching_class_grade g WHERE g.teaching_class_id=tc.id AND g.grade_id=st.grade_id);

SELECT '=== 9. 脏数据 / 学期时间轴 ===' AS section;
SELECT 'course 脏码/名' AS item, COUNT(*) c FROM course
 WHERE course_code REGEXP '(111|222|333|666|test|Test)' OR course_name REGEXP '(test|Test|测试|张三|李四|王五)'
UNION ALL SELECT 'teaching_class 脏码', COUNT(*) FROM teaching_class
 WHERE class_code REGEXP '(111111|22222|33333|666666|2027Java|c011|test)'
UNION ALL SELECT 'semester 脏码', COUNT(*) FROM semester WHERE semester_code='111111'
UNION ALL SELECT 'batch 脏名', COUNT(*) FROM selection_batch WHERE batch_name IN ('1111','2027-2028-1')
UNION ALL SELECT 'student 11111', COUNT(*) FROM student WHERE student_no='11111' AND student_name='11111'
UNION ALL SELECT 'student_name 占位', COUNT(*) FROM student WHERE student_name REGEXP '^(test|Test|测试|张三|李四|王五|张老师)$';

SELECT '--- 9.1 学期时间轴 ---' AS note;
SELECT id, semester_code, start_date, end_date, status FROM semester ORDER BY start_date;
SELECT '--- 9.2 日期重叠对（应为空） ---' AS note2;
SELECT a.id a_id, a.semester_code a_code, b.id b_id, b.semester_code b_code
FROM semester a JOIN semester b ON a.id<b.id
WHERE a.start_date < b.end_date AND b.start_date < a.end_date;
SELECT '--- 9.3 状态与日期矛盾（应为空） ---' AS note3;
SELECT * FROM (
  SELECT id, semester_code, start_date, end_date, status,
    CASE WHEN status='FINISHED' AND end_date >= CURDATE() THEN 'FINISHED_NOT_ENDED'
         WHEN status='ACTIVE' AND NOT (start_date<=CURDATE() AND end_date>=CURDATE()) THEN 'ACTIVE_NOT_SPANNING'
         WHEN status='PLANNED' AND start_date<=CURDATE() THEN 'PLANNED_STARTED'
         ELSE 'OK' END v
  FROM semester) t WHERE v<>'OK';

SELECT '=== 10. 成绩分布重算 ===' AS section;
SELECT COUNT(*) n, ROUND(AVG(score_value),2) avg_score, ROUND(STDDEV_POP(score_value),2) sd,
  ROUND(100*SUM(score_value>=60)/COUNT(*),2) pass_pct,
  ROUND(100*SUM(score_value>=85)/COUNT(*),2) exc_pct,
  MIN(score_value) min_s, MAX(score_value) max_s
FROM score;

SELECT '--- 10.1 五分段 ---' AS note;
SELECT CASE WHEN score_value<60 THEN '1. 0-59' WHEN score_value<70 THEN '2. 60-69'
            WHEN score_value<80 THEN '3. 70-79' WHEN score_value<90 THEN '4. 80-89'
            ELSE '5. 90-100' END band, COUNT(*) c,
       ROUND(100*COUNT(*)/(SELECT COUNT(*) FROM score),2) pct
FROM score GROUP BY band ORDER BY band;

SELECT '--- 10.2 课程均分极差 ---' AS note2;
SELECT COUNT(*) course_cnt, ROUND(MIN(a),2) min_avg, ROUND(MAX(a),2) max_avg,
       ROUND(MAX(a)-MIN(a),2) range_avg, ROUND(STDDEV_POP(a),2) sd_of_avg
FROM (SELECT c.id, AVG(s.score_value) a FROM score s
      JOIN course_selection cs ON cs.id=s.course_selection_id
      JOIN teaching_class tc ON tc.id=cs.teaching_class_id
      JOIN course c ON c.id=tc.course_id
      GROUP BY c.id HAVING COUNT(*)>=100) t;

SELECT '=== 11. 毕业风险分档（sem5 学生口径） ===' AS section;
SELECT risk_level, COUNT(*) students,
       ROUND(100*COUNT(*)/(SELECT COUNT(*) FROM stat_credit_progress WHERE semester_id=5),2) pct
FROM stat_credit_progress WHERE semester_id=5 GROUP BY risk_level ORDER BY students DESC;

SELECT '--- 11.1 年级 × risk ---' AS note;
SELECT gc.grade_name, COUNT(*) total,
  SUM(p.risk_level='HIGH') high_n, SUM(p.risk_level='MEDIUM') med_n, SUM(p.risk_level='LOW') low_n,
  ROUND(100*SUM(p.risk_level='HIGH')/COUNT(*),2) high_pct
FROM stat_credit_progress p JOIN grade_cohort gc ON gc.id=p.grade_id
WHERE p.semester_id=5 GROUP BY gc.grade_name, gc.entry_year ORDER BY gc.entry_year;

SELECT '=== 12. 满班率 / 教室利用率 / 教师工作量 ===' AS section;
SELECT semester_id, COUNT(*) total, SUM(selected_count>=capacity AND capacity>0) full_n,
       ROUND(100*SUM(selected_count>=capacity AND capacity>0)/COUNT(*),2) full_pct,
       SUM(capacity=0) cap_zero, SUM(selected_count>capacity) over_cap
FROM teaching_class GROUP BY semester_id ORDER BY semester_id;

SELECT '--- 12.1 教室利用率 ---' AS note;
SELECT semester_id, COUNT(*) n, MIN(usage_rate) min_r, MAX(usage_rate) max_r,
       ROUND(AVG(usage_rate),2) avg_r, SUM(usage_rate>100) over_100
FROM stat_classroom_usage GROUP BY semester_id ORDER BY semester_id;

SELECT '--- 12.2 教师工作量 ---' AS note2;
SELECT semester_id, COUNT(*) teacher_n, MIN(total_hours) min_h, MAX(total_hours) max_h,
       ROUND(AVG(total_hours),1) avg_h, SUM(total_hours=0) zero_h
FROM stat_teacher_workload GROUP BY semester_id ORDER BY semester_id;

SELECT '=== 13. stat_semester_overview withdraw_count ===' AS section;
SELECT o.semester_id, s.semester_code, o.selection_count o_sel, o.waitlist_count o_wai, o.withdraw_count o_wdr,
       b.sel real_sel, b.wdr real_wdr
FROM stat_semester_overview o
JOIN semester s ON s.id=o.semester_id
JOIN (SELECT tc.semester_id sid, SUM(cs.status='SELECTED') sel, SUM(cs.status='WITHDRAWN') wdr
      FROM course_selection cs JOIN teaching_class tc ON tc.id=cs.teaching_class_id
      GROUP BY tc.semester_id) b ON b.sid=o.semester_id
ORDER BY s.start_date;

SELECT '=== 3. S-01 stat_credit_progress 按学期演进 ===' AS section;
SELECT cnt_distinct, COUNT(*) students FROM (
  SELECT student_id, COUNT(DISTINCT progress_rate) cnt_distinct
  FROM stat_credit_progress GROUP BY student_id) t
GROUP BY cnt_distinct ORDER BY cnt_distinct;

SELECT '--- 3.1 样例：跨学期 completed_credit 是否递增 ---' AS note;
SELECT p.student_id, s.semester_code, p.completed_credit, p.passed_credit, p.progress_rate, p.risk_level
FROM stat_credit_progress p JOIN semester s ON s.id=p.semester_id
WHERE p.student_id IN (1, 700)
ORDER BY p.student_id, s.start_date;

SELECT '--- 3.2 每学期完成学分的整体水位（应递增） ---' AS note2;
SELECT s.semester_code, COUNT(*) n, ROUND(AVG(p.completed_credit),2) avg_completed,
       ROUND(AVG(p.progress_rate),2) avg_progress
FROM stat_credit_progress p JOIN semester s ON s.id=p.semester_id
GROUP BY s.semester_code, s.start_date ORDER BY s.start_date;
