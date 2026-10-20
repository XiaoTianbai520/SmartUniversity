-- 精确冲突检测：同教室/同教师 + 同 weekday + 节次重叠 + 周次重叠
USE edu_course_selection;

SELECT '=== C1 教室冲突（周次区间也重叠） ===' AS section;
SELECT COUNT(*) AS conflict_pairs, COUNT(DISTINCT a.classroom_id) AS classrooms_involved
FROM teaching_class_schedule a
JOIN teaching_class_schedule b
  ON a.classroom_id = b.classroom_id
 AND a.weekday = b.weekday
 AND a.teaching_class_id < b.teaching_class_id
 AND a.start_section <= b.end_section AND b.start_section <= a.end_section
 AND a.start_week    <= b.end_week    AND b.start_week    <= a.end_week
JOIN teaching_class ta ON ta.id = a.teaching_class_id
JOIN teaching_class tb ON tb.id = b.teaching_class_id
WHERE ta.semester_id = 5 AND tb.semester_id = 5;

SELECT '--- C1.1 涉及的教室 id 列表 ---' AS note;
SELECT GROUP_CONCAT(DISTINCT a.classroom_id ORDER BY a.classroom_id) AS classrooms
FROM teaching_class_schedule a
JOIN teaching_class_schedule b
  ON a.classroom_id = b.classroom_id AND a.weekday = b.weekday
 AND a.teaching_class_id < b.teaching_class_id
 AND a.start_section <= b.end_section AND b.start_section <= a.end_section
 AND a.start_week    <= b.end_week    AND b.start_week    <= a.end_week
JOIN teaching_class ta ON ta.id = a.teaching_class_id
JOIN teaching_class tb ON tb.id = b.teaching_class_id
WHERE ta.semester_id = 5 AND tb.semester_id = 5;

SELECT '=== C2 教师冲突（周次区间也重叠） ===' AS section;
SELECT COUNT(*) AS conflict_pairs, COUNT(DISTINCT ta.teacher_id) AS teachers_involved
FROM teaching_class_schedule a
JOIN teaching_class_schedule b
  ON a.weekday = b.weekday
 AND a.teaching_class_id < b.teaching_class_id
 AND a.start_section <= b.end_section AND b.start_section <= a.end_section
 AND a.start_week    <= b.end_week    AND b.start_week    <= a.end_week
JOIN teaching_class ta ON ta.id = a.teaching_class_id
JOIN teaching_class tb ON tb.id = b.teaching_class_id
WHERE ta.semester_id = 5 AND tb.semester_id = 5 AND ta.teacher_id = tb.teacher_id;

SELECT '=== C3 巡检覆盖度对比 ===' AS section;
SELECT '教室：真实涉及教室数 vs 巡检记录数' AS metric,
 (SELECT COUNT(DISTINCT a.classroom_id) FROM teaching_class_schedule a
   JOIN teaching_class_schedule b ON a.classroom_id=b.classroom_id AND a.weekday=b.weekday
     AND a.teaching_class_id<b.teaching_class_id
     AND a.start_section<=b.end_section AND b.start_section<=a.end_section
     AND a.start_week<=b.end_week AND b.start_week<=a.end_week
   JOIN teaching_class ta ON ta.id=a.teaching_class_id JOIN teaching_class tb ON tb.id=b.teaching_class_id
   WHERE ta.semester_id=5 AND tb.semester_id=5) AS real_cnt,
 (SELECT COUNT(DISTINCT biz_id) FROM data_audit_issue WHERE biz_type='CLASSROOM') AS audit_cnt
UNION ALL
SELECT '教师：真实涉及教师数 vs 巡检记录数',
 (SELECT COUNT(DISTINCT ta.teacher_id) FROM teaching_class_schedule a
   JOIN teaching_class_schedule b ON a.weekday=b.weekday AND a.teaching_class_id<b.teaching_class_id
     AND a.start_section<=b.end_section AND b.start_section<=a.end_section
     AND a.start_week<=b.end_week AND b.start_week<=a.end_week
   JOIN teaching_class ta ON ta.id=a.teaching_class_id JOIN teaching_class tb ON tb.id=b.teaching_class_id
   WHERE ta.semester_id=5 AND tb.semester_id=5 AND ta.teacher_id=tb.teacher_id),
 (SELECT COUNT(DISTINCT biz_id) FROM data_audit_issue WHERE biz_type='TEACHER');

SELECT '=== C4 被漏报的真实冲突教室/教师（巡检里没有的） ===' AS section;
SELECT '漏报教室' AS kind, a.classroom_id AS id
FROM teaching_class_schedule a
JOIN teaching_class_schedule b ON a.classroom_id=b.classroom_id AND a.weekday=b.weekday
  AND a.teaching_class_id<b.teaching_class_id
  AND a.start_section<=b.end_section AND b.start_section<=a.end_section
  AND a.start_week<=b.end_week AND b.start_week<=a.end_week
JOIN teaching_class ta ON ta.id=a.teaching_class_id JOIN teaching_class tb ON tb.id=b.teaching_class_id
WHERE ta.semester_id=5 AND tb.semester_id=5
  AND a.classroom_id NOT IN (SELECT biz_id FROM data_audit_issue WHERE biz_type='CLASSROOM')
GROUP BY a.classroom_id
UNION
SELECT '漏报教师', ta.teacher_id
FROM teaching_class_schedule a
JOIN teaching_class_schedule b ON a.weekday=b.weekday AND a.teaching_class_id<b.teaching_class_id
  AND a.start_section<=b.end_section AND b.start_section<=a.end_section
  AND a.start_week<=b.end_week AND b.start_week<=a.end_week
JOIN teaching_class ta ON ta.id=a.teaching_class_id JOIN teaching_class tb ON tb.id=b.teaching_class_id
WHERE ta.semester_id=5 AND tb.semester_id=5 AND ta.teacher_id=tb.teacher_id
  AND ta.teacher_id NOT IN (SELECT biz_id FROM data_audit_issue WHERE biz_type='TEACHER')
GROUP BY ta.teacher_id;

SELECT '=== C5 回溯验证 2 条教室冲突 + 2 条教师冲突 ===' AS section;
SELECT '--- 教室 28 周3 第9节 ---' AS note;
SELECT s.teaching_class_id, s.classroom_id, s.weekday, s.start_section, s.end_section, s.start_week, s.end_week, tc.teacher_id
FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
WHERE tc.semester_id=5 AND s.classroom_id=28 AND s.weekday=3 AND s.start_section=9;

SELECT '--- 教室 33 周3 第1节 ---' AS note;
SELECT s.teaching_class_id, s.classroom_id, s.weekday, s.start_section, s.end_section, s.start_week, s.end_week, tc.teacher_id
FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
WHERE tc.semester_id=5 AND s.classroom_id=33 AND s.weekday=3 AND s.start_section=1;

SELECT '--- 教师 13：真实冲突槽位 ---' AS note;
SELECT s.teaching_class_id, tc.teacher_id, s.classroom_id, s.weekday, s.start_section, s.end_section, s.start_week, s.end_week
FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
WHERE tc.semester_id=5 AND tc.teacher_id=13 ORDER BY s.weekday, s.start_section;

SELECT '--- 教师 41：真实冲突槽位 ---' AS note;
SELECT s.teaching_class_id, tc.teacher_id, s.classroom_id, s.weekday, s.start_section, s.end_section, s.start_week, s.end_week
FROM teaching_class_schedule s JOIN teaching_class tc ON tc.id=s.teaching_class_id
WHERE tc.semester_id=5 AND tc.teacher_id=41 ORDER BY s.weekday, s.start_section;
