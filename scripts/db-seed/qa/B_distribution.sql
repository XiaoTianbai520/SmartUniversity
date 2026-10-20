-- B9~B16 分布与区分度校验（只读）
USE edu_course_selection;

SELECT '=== B9 总体成绩分布 ===' AS section;
SELECT COUNT(*) AS n,
       ROUND(AVG(score_value),2) AS avg_score,
       ROUND(STDDEV_POP(score_value),2) AS std_pop,
       ROUND(STDDEV_SAMP(score_value),2) AS std_samp,
       ROUND(100*SUM(score_value>=60)/COUNT(*),2) AS pass_rate_pct,
       ROUND(100*SUM(score_value>=85)/COUNT(*),2) AS excellent_rate_pct,
       MIN(score_value) AS min_score, MAX(score_value) AS max_score
FROM score;

SELECT '--- B9.1 五分段 ---' AS note;
SELECT CASE WHEN score_value<60 THEN '1. 0-59'
            WHEN score_value<70 THEN '2. 60-69'
            WHEN score_value<80 THEN '3. 70-79'
            WHEN score_value<90 THEN '4. 80-89'
            ELSE '5. 90-100' END AS band,
       COUNT(*) AS cnt,
       ROUND(100*COUNT(*)/(SELECT COUNT(*) FROM score),2) AS pct
FROM score GROUP BY band ORDER BY band;

SELECT '=== B9.2 按课程均分极差 top5 / bottom5 ===' AS section;
SELECT '--- TOP5 高分课 ---' AS note;
SELECT c.id AS course_id, c.course_code, c.course_name, COUNT(*) AS n,
       ROUND(AVG(s.score_value),2) AS avg_score, ROUND(STDDEV_POP(s.score_value),2) AS sd
FROM score s
JOIN course_selection cs ON cs.id=s.course_selection_id
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN course c ON c.id=tc.course_id
GROUP BY c.id, c.course_code, c.course_name
HAVING COUNT(*)>=100
ORDER BY avg_score DESC LIMIT 5;

SELECT '--- BOTTOM5 低分课 ---' AS note;
SELECT c.id AS course_id, c.course_code, c.course_name, COUNT(*) AS n,
       ROUND(AVG(s.score_value),2) AS avg_score, ROUND(STDDEV_POP(s.score_value),2) AS sd
FROM score s
JOIN course_selection cs ON cs.id=s.course_selection_id
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN course c ON c.id=tc.course_id
GROUP BY c.id, c.course_code, c.course_name
HAVING COUNT(*)>=100
ORDER BY avg_score ASC LIMIT 5;

SELECT '--- B9.3 按课程均分的整体分布（极差/标准差） ---' AS note;
SELECT COUNT(*) AS course_cnt,
       ROUND(MIN(avg_score),2) AS min_course_avg,
       ROUND(MAX(avg_score),2) AS max_course_avg,
       ROUND(MAX(avg_score)-MIN(avg_score),2) AS range_avg,
       ROUND(STDDEV_POP(avg_score),2) AS sd_of_course_avg
FROM (SELECT c.id, AVG(s.score_value) avg_score FROM score s
      JOIN course_selection cs ON cs.id=s.course_selection_id
      JOIN teaching_class tc ON tc.id=cs.teaching_class_id
      JOIN course c ON c.id=tc.course_id
      GROUP BY c.id HAVING COUNT(*)>=100) t;

SELECT '=== B10 同一门课跨学期均分稳定性（3 门课） ===' AS section;
SELECT c.course_code, c.course_name, tc.semester_id, sm.semester_code,
       COUNT(*) AS n, ROUND(AVG(s.score_value),2) AS avg_score,
       ROUND(STDDEV_POP(s.score_value),2) AS sd
FROM score s
JOIN course_selection cs ON cs.id=s.course_selection_id
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN course c ON c.id=tc.course_id
JOIN semester sm ON sm.id=tc.semester_id
WHERE c.course_code IN (
  SELECT cc FROM (SELECT c2.course_code cc, COUNT(DISTINCT tc2.semester_id) sc
                  FROM teaching_class tc2 JOIN course c2 ON c2.id=tc2.course_id
                  GROUP BY c2.course_code HAVING sc>=8 LIMIT 3) z)
GROUP BY c.course_code, c.course_name, tc.semester_id, sm.semester_code
ORDER BY c.course_code, sm.start_date;

SELECT '=== B11 stat_credit_progress progress_rate 分桶 ===' AS section;
SELECT CASE WHEN progress_rate < 30 THEN 'a. <30'
            WHEN progress_rate < 60 THEN 'b. 30-60'
            WHEN progress_rate < 80 THEN 'c. 60-80'
            WHEN progress_rate <= 100 THEN 'd. 80-100'
            ELSE 'e. >100' END AS bucket,
       COUNT(*) AS cnt,
       ROUND(100*COUNT(*)/(SELECT COUNT(*) FROM stat_credit_progress),2) AS pct,
       ROUND(MIN(progress_rate),2) AS bmin, ROUND(MAX(progress_rate),2) AS bmax
FROM stat_credit_progress GROUP BY bucket ORDER BY bucket;

SELECT '--- B11.1 risk_level 分布 ---' AS note;
SELECT risk_level, COUNT(*) AS cnt,
       ROUND(100*COUNT(*)/(SELECT COUNT(*) FROM stat_credit_progress),2) AS pct
FROM stat_credit_progress GROUP BY risk_level ORDER BY cnt DESC;

SELECT '=== B12 毕业风险分档：年级 × risk_level ===' AS section;
SELECT gc.grade_name, gc.entry_year, scp.risk_level, COUNT(*) AS student_cnt
FROM stat_credit_progress scp
JOIN grade_cohort gc ON gc.id = scp.grade_id
GROUP BY gc.grade_name, gc.entry_year, scp.risk_level
ORDER BY gc.entry_year, scp.risk_level;

SELECT '--- B12.1 年级 × risk 交叉（行转列） ---' AS note;
SELECT gc.grade_name, gc.entry_year, COUNT(*) AS total,
  SUM(scp.risk_level='HIGH')   AS high_cnt,
  SUM(scp.risk_level='MEDIUM') AS medium_cnt,
  SUM(scp.risk_level='LOW')    AS low_cnt,
  ROUND(100*SUM(scp.risk_level='HIGH')/COUNT(*),2) AS high_pct
FROM stat_credit_progress scp JOIN grade_cohort gc ON gc.id=scp.grade_id
GROUP BY gc.grade_name, gc.entry_year ORDER BY gc.entry_year;

SELECT '--- B12.2 同口径复核：直接由 score/course 算 risk（不读快照） ---' AS note;
SELECT gc.grade_name,
       ROUND(AVG(t.credit),1) AS avg_passed_credit,
       COUNT(*) AS student_cnt
FROM (
  SELECT st.id, st.grade_id, COALESCE(SUM(CASE WHEN s.score_value>=60 THEN c.credit ELSE 0 END),0) credit
  FROM student st
  LEFT JOIN course_selection cs ON cs.student_id=st.id AND cs.status='SELECTED'
  LEFT JOIN score s ON s.course_selection_id=cs.id
  LEFT JOIN teaching_class tc ON tc.id=cs.teaching_class_id
  LEFT JOIN course c ON c.id=tc.course_id
  GROUP BY st.id, st.grade_id
) t JOIN grade_cohort gc ON gc.id=t.grade_id
GROUP BY gc.grade_name ORDER BY gc.grade_name;

SELECT '=== B13 满班率 ===' AS section;
SELECT COUNT(*) AS total_class,
       SUM(selected_count >= capacity AND capacity>0) AS full_class,
       ROUND(100*SUM(selected_count >= capacity AND capacity>0)/COUNT(*),2) AS full_pct,
       SUM(capacity=0) AS capacity_zero,
       SUM(selected_count > capacity) AS over_capacity
FROM teaching_class WHERE semester_id=5;

SELECT '--- B13.1 各学期满班率 ---' AS note;
SELECT semester_id, COUNT(*) total, SUM(selected_count>=capacity AND capacity>0) full_n,
       ROUND(100*SUM(selected_count>=capacity AND capacity>0)/COUNT(*),2) full_pct
FROM teaching_class GROUP BY semester_id ORDER BY semester_id;

SELECT '--- B13.2 stat_course_selection full_flag 统计 ---' AS note;
SELECT semester_id, SUM(full_flag=1) AS full_flag_n, COUNT(*) AS total,
       ROUND(100*SUM(full_flag=1)/COUNT(*),2) AS pct
FROM stat_course_selection GROUP BY semester_id ORDER BY semester_id;

SELECT '=== B14 stat_classroom_usage 利用率 ===' AS section;
SELECT semester_id, COUNT(*) n, ROUND(MIN(usage_rate),2) min_rate,
       ROUND(MAX(usage_rate),2) max_rate, ROUND(AVG(usage_rate),2) avg_rate
FROM stat_classroom_usage GROUP BY semester_id ORDER BY semester_id;

SELECT '--- B14.1 sem5 分位数 ---' AS note;
SELECT room_cnt, MAX(usage_rate) AS max_rate,
  (SELECT ROUND(usage_rate,2) FROM stat_classroom_usage WHERE semester_id=5 ORDER BY usage_rate LIMIT 1 OFFSET 29) AS median_rate,
  MIN(usage_rate) AS min_rate,
  SUM(usage_rate>100) AS over_100,
  SUM(usage_hours>total_hours) AS hours_exceed
FROM stat_classroom_usage, (SELECT COUNT(*) room_cnt FROM stat_classroom_usage WHERE semester_id=5) z
WHERE semester_id=5 GROUP BY room_cnt;

SELECT '--- B14.2 usage_hours vs total_hours 一致性样例 ---' AS note;
SELECT classroom_id, building_name, room_no, usage_hours, total_hours, usage_rate,
       ROUND(100*usage_hours/total_hours,2) AS calc_rate
FROM stat_classroom_usage WHERE semester_id=5 ORDER BY usage_rate DESC LIMIT 5;

SELECT '=== B15 stat_teacher_workload 梯度 ===' AS section;
SELECT semester_id, COUNT(*) teacher_n, MIN(class_count) min_cls, MAX(class_count) max_cls,
       MIN(total_hours) min_hours, MAX(total_hours) max_hours, ROUND(AVG(total_hours),1) avg_hours,
       SUM(class_count=0) zero_class_teacher, SUM(total_hours=0) zero_hours_teacher
FROM stat_teacher_workload GROUP BY semester_id ORDER BY semester_id;

SELECT '--- B15.1 sem5 工作量分位数 ---' AS note;
SELECT 'p25' q, (SELECT total_hours FROM stat_teacher_workload WHERE semester_id=5 ORDER BY total_hours LIMIT 1 OFFSET 38) v
UNION ALL SELECT 'p50', (SELECT total_hours FROM stat_teacher_workload WHERE semester_id=5 ORDER BY total_hours LIMIT 1 OFFSET 77)
UNION ALL SELECT 'p75', (SELECT total_hours FROM stat_teacher_workload WHERE semester_id=5 ORDER BY total_hours LIMIT 1 OFFSET 116);

SELECT '=== B16 data_audit_issue 按 rule_code 分组 ===' AS section;
SELECT rule_code, rule_name, severity, COUNT(*) AS cnt,
       COUNT(DISTINCT biz_id) AS distinct_biz, MIN(semester_id) AS sem_min, MAX(semester_id) AS sem_max
FROM data_audit_issue GROUP BY rule_code, rule_name, severity ORDER BY cnt DESC;

SELECT '--- B16.1 教室冲突明细（前 10） ---' AS note;
SELECT id, rule_code, biz_type, biz_id, semester_id, LEFT(description,120) AS descr
FROM data_audit_issue WHERE rule_code LIKE '%CONFLICT%' AND description LIKE '%教室%'
   OR (rule_code LIKE '%CONFLICT%' AND biz_type LIKE '%CLASSROOM%')
ORDER BY id LIMIT 10;

SELECT '--- B16.2 全部冲突条目的 biz_type 分布 ---' AS note;
SELECT rule_code, biz_type, COUNT(*) cnt FROM data_audit_issue GROUP BY rule_code, biz_type;
