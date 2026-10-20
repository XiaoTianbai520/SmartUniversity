-- A2~A8 一致性校验（只读）
USE edu_course_selection;

SELECT '=== A2 selected_count 冗余列 vs 实际 SELECTED 条数 ===' AS section;
SELECT COUNT(*) AS bad_class_rows,
       SUM(CASE WHEN diff > 0 THEN 1 ELSE 0 END) AS redundant_over,
       SUM(CASE WHEN diff < 0 THEN 1 ELSE 0 END) AS redundant_under,
       MAX(ABS(diff)) AS max_abs_diff
FROM (
  SELECT tc.id, tc.selected_count,
         (SELECT COUNT(*) FROM course_selection cs
           WHERE cs.teaching_class_id = tc.id AND cs.status='SELECTED') AS real_cnt,
         tc.selected_count - (SELECT COUNT(*) FROM course_selection cs
           WHERE cs.teaching_class_id = tc.id AND cs.status='SELECTED') AS diff
  FROM teaching_class tc
) t
WHERE diff <> 0;

SELECT '--- A2 样例(最多20条) ---' AS note;
SELECT tc.id AS class_id, tc.class_code, tc.semester_id, tc.capacity,
       tc.selected_count AS stored_cnt, 
       (SELECT COUNT(*) FROM course_selection cs WHERE cs.teaching_class_id=tc.id AND cs.status='SELECTED') AS real_cnt
FROM teaching_class tc
WHERE tc.selected_count <> (SELECT COUNT(*) FROM course_selection cs WHERE cs.teaching_class_id=tc.id AND cs.status='SELECTED')
ORDER BY tc.id LIMIT 20;

SELECT '=== A3 (student_id, teaching_class_id) 重复 ===' AS section;
SELECT COUNT(*) AS dup_groups, COALESCE(SUM(c-1),0) AS extra_rows FROM (
  SELECT student_id, teaching_class_id, COUNT(*) c
  FROM course_selection GROUP BY student_id, teaching_class_id HAVING COUNT(*)>1
) t;
SELECT '--- A3 样例 ---' AS note;
SELECT student_id, teaching_class_id, COUNT(*) c, GROUP_CONCAT(status) statuses
FROM course_selection GROUP BY student_id, teaching_class_id HAVING COUNT(*)>1 LIMIT 10;

SELECT '=== A4 SELECTED 选课是否都满足 major/grade 适用范围 ===' AS section;
SELECT 'major 不匹配的 SELECTED 条数' AS item, COUNT(*) AS cnt
FROM course_selection cs
JOIN teaching_class tc ON tc.id = cs.teaching_class_id
JOIN student st ON st.id = cs.student_id
WHERE cs.status='SELECTED'
  AND NOT EXISTS (SELECT 1 FROM teaching_class_major m WHERE m.teaching_class_id=tc.id AND m.major_id=st.major_id);

SELECT 'grade 不匹配的 SELECTED 条数' AS item, COUNT(*) AS cnt
FROM course_selection cs
JOIN teaching_class tc ON tc.id = cs.teaching_class_id
JOIN student st ON st.id = cs.student_id
WHERE cs.status='SELECTED'
  AND NOT EXISTS (SELECT 1 FROM teaching_class_grade g WHERE g.teaching_class_id=tc.id AND g.grade_id=st.grade_id);

SELECT '无任何 major 范围记录的班(应为0)' AS item, COUNT(*) AS cnt FROM teaching_class tc
WHERE NOT EXISTS (SELECT 1 FROM teaching_class_major m WHERE m.teaching_class_id=tc.id);
SELECT '无任何 grade 范围记录的班(应为0)' AS item, COUNT(*) AS cnt FROM teaching_class tc
WHERE NOT EXISTS (SELECT 1 FROM teaching_class_grade g WHERE g.teaching_class_id=tc.id);

SELECT '--- A4 违规样例 ---' AS note;
SELECT cs.id AS selection_id, cs.student_id, st.major_id AS stu_major, st.grade_id AS stu_grade,
       cs.teaching_class_id, tc.semester_id
FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN student st ON st.id=cs.student_id
WHERE cs.status='SELECTED'
  AND ( NOT EXISTS (SELECT 1 FROM teaching_class_major m WHERE m.teaching_class_id=tc.id AND m.major_id=st.major_id)
     OR NOT EXISTS (SELECT 1 FROM teaching_class_grade g WHERE g.teaching_class_id=tc.id AND g.grade_id=st.grade_id) )
LIMIT 10;

SELECT '=== A5 学生每学期 SELECTED 总学分 vs max_selection_credit ===' AS section;
SELECT COUNT(*) AS over_limit_rows FROM (
  SELECT cs.student_id, tc.semester_id, SUM(c.credit) AS total_credit, s.max_selection_credit
  FROM course_selection cs
  JOIN teaching_class tc ON tc.id=cs.teaching_class_id
  JOIN course c ON c.id=tc.course_id
  JOIN semester s ON s.id=tc.semester_id
  WHERE cs.status='SELECTED'
  GROUP BY cs.student_id, tc.semester_id, s.max_selection_credit
) t WHERE total_credit > max_selection_credit;

SELECT '--- A5 超限样例 ---' AS note;
SELECT cs.student_id, tc.semester_id, SUM(c.credit) total_credit, s.max_selection_credit
FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN course c ON c.id=tc.course_id
JOIN semester s ON s.id=tc.semester_id
WHERE cs.status='SELECTED'
GROUP BY cs.student_id, tc.semester_id, s.max_selection_credit
HAVING total_credit > s.max_selection_credit ORDER BY total_credit DESC LIMIT 10;

SELECT '=== A6 score 是否只挂在 SELECTED 上 ===' AS section;
SELECT cs.status AS selection_status, COUNT(*) AS score_rows
FROM score sc JOIN course_selection cs ON cs.id=sc.course_selection_id
GROUP BY cs.status;

SELECT '--- A6 悬浮 score（course_selection_id 无主） ---' AS note;
SELECT COUNT(*) AS orphan_score FROM score sc
WHERE NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.id=sc.course_selection_id);

SELECT '=== A7 semester 时间轴自洽 ===' AS section;
SELECT id, semester_code, academic_year, term_no, start_date, end_date, status, max_selection_credit,
       DATEDIFF(end_date, start_date) AS span_days, CURDATE() AS today
FROM semester ORDER BY start_date;

SELECT '--- A7.1 相邻学期日期重叠对 ---' AS note;
SELECT a.id AS a_id, a.semester_code AS a_code, a.start_date a_start, a.end_date a_end,
       b.id AS b_id, b.semester_code AS b_code, b.start_date b_start, b.end_date b_end
FROM semester a JOIN semester b ON a.id < b.id
WHERE a.start_date < b.end_date AND b.start_date < a.end_date;

SELECT '--- A7.2 状态与日期矛盾（FINISHED 但 end>=today / ACTIVE 不跨今天 / PLANNED 已开始） ---' AS note;
SELECT * FROM (
  SELECT id, semester_code, start_date, end_date, status,
    CASE
      WHEN status='FINISHED' AND end_date >= CURDATE() THEN 'FINISHED_BUT_NOT_ENDED'
      WHEN status='ACTIVE' AND NOT (start_date <= CURDATE() AND end_date >= CURDATE()) THEN 'ACTIVE_NOT_SPANNING_TODAY'
      WHEN status='PLANNED' AND start_date <= CURDATE() THEN 'PLANNED_ALREADY_STARTED'
      ELSE 'OK'
    END AS verdict
  FROM semester
) v WHERE v.verdict <> 'OK';

SELECT '--- A7.3 PLANNED 学期是否真的无教学班 ---' AS note;
SELECT s.id, s.semester_code, s.status, COUNT(tc.id) AS class_cnt
FROM semester s LEFT JOIN teaching_class tc ON tc.semester_id=s.id
GROUP BY s.id, s.semester_code, s.status ORDER BY s.start_date;

SELECT '=== A8 外键悬空 ===' AS section;
SELECT 'schedule.classroom_id 悬空' AS item, COUNT(*) AS cnt FROM teaching_class_schedule x
  WHERE x.classroom_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM classroom c WHERE c.id=x.classroom_id)
UNION ALL SELECT 'schedule.teaching_class_id 悬空', COUNT(*) FROM teaching_class_schedule x
  WHERE NOT EXISTS (SELECT 1 FROM teaching_class t WHERE t.id=x.teaching_class_id)
UNION ALL SELECT 'course_selection.batch_id 悬空', COUNT(*) FROM course_selection x
  WHERE x.batch_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM selection_batch b WHERE b.id=x.batch_id)
UNION ALL SELECT 'course_selection.student_id 悬空', COUNT(*) FROM course_selection x
  WHERE NOT EXISTS (SELECT 1 FROM student s WHERE s.id=x.student_id)
UNION ALL SELECT 'course_selection.teaching_class_id 悬空', COUNT(*) FROM course_selection x
  WHERE NOT EXISTS (SELECT 1 FROM teaching_class t WHERE t.id=x.teaching_class_id)
UNION ALL SELECT 'score.course_selection_id 悬空', COUNT(*) FROM score x
  WHERE NOT EXISTS (SELECT 1 FROM course_selection s WHERE s.id=x.course_selection_id)
UNION ALL SELECT 'teaching_class.course_id 悬空', COUNT(*) FROM teaching_class x
  WHERE NOT EXISTS (SELECT 1 FROM course c WHERE c.id=x.course_id)
UNION ALL SELECT 'teaching_class.teacher_id 悬空', COUNT(*) FROM teaching_class x
  WHERE x.teacher_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM teacher t WHERE t.id=x.teacher_id)
UNION ALL SELECT 'teaching_class.semester_id 悬空', COUNT(*) FROM teaching_class x
  WHERE NOT EXISTS (SELECT 1 FROM semester s WHERE s.id=x.semester_id);

SELECT '--- A8.b 选课 batch 与教学班学期是否匹配 ---' AS note;
SELECT COUNT(*) AS batch_semester_mismatch FROM course_selection cs
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN selection_batch b ON b.id=cs.batch_id
WHERE b.semester_id <> tc.semester_id;
