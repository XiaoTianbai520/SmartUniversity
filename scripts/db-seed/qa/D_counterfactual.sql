-- 反事实测算：若 remainingSemesters 按「真实剩余学期数」计算，HIGH/MEDIUM/LOW 分布会怎样
USE edu_course_selection;

SELECT '=== 反事实对比（sem5 快照） ===' AS section;
SELECT
  SUM(plan_total_credit - completed_credit <= 0) AS gap_le_0,
  SUM(plan_total_credit - completed_credit > 0
      AND (plan_total_credit - completed_credit) / 1 > 24) AS not_used
FROM stat_credit_progress WHERE semester_id = 5;

-- 现口径：remaining = 4 - (2026 - entry_year)，最小 1（年当学期用，缺陷口径）
SELECT '--- 现口径风险分布（按代码 4-(2026-entryYear) 复算） ---' AS note;
SELECT risk_level_calc, COUNT(*) n FROM (
  SELECT CASE
    WHEN plan_total_credit - completed_credit <= 0 THEN 'LOW'
    WHEN (plan_total_credit - completed_credit) / GREATEST(4 - (2026 - gc.entry_year), 1) > 24 THEN 'HIGH'
    WHEN (plan_total_credit - completed_credit) / GREATEST(4 - (2026 - gc.entry_year), 1) > 18 THEN 'MEDIUM'
    ELSE 'LOW' END AS risk_level_calc
  FROM stat_credit_progress p JOIN grade_cohort gc ON gc.id = p.grade_id
  WHERE p.semester_id = 5) t
GROUP BY risk_level_calc ORDER BY n DESC;

-- 正确口径：2026-2027-1 为 2026 级第 1 学期 => 剩余 = 8 - (已修学期数) = 2*(4-(2026-entry)) 且至少 1
SELECT '--- 正确口径（remaining = 2*(4-(2026-entryYear)) ）风险分布 ---' AS note;
SELECT risk_level_calc, COUNT(*) n FROM (
  SELECT CASE
    WHEN plan_total_credit - completed_credit <= 0 THEN 'LOW'
    WHEN (plan_total_credit - completed_credit) / GREATEST(2 * (4 - (2026 - gc.entry_year)), 1) > 24 THEN 'HIGH'
    WHEN (plan_total_credit - completed_credit) / GREATEST(2 * (4 - (2026 - gc.entry_year)), 1) > 18 THEN 'MEDIUM'
    ELSE 'LOW' END AS risk_level_calc
  FROM stat_credit_progress p JOIN grade_cohort gc ON gc.id = p.grade_id
  WHERE p.semester_id = 5) t
GROUP BY risk_level_calc ORDER BY n DESC;

SELECT '--- 按年级：现口径 vs 正确口径 HIGH 人数 ---' AS note;
SELECT gc.grade_name,
  SUM(CASE WHEN p.plan_total_credit - p.completed_credit > 0
        AND (p.plan_total_credit - p.completed_credit)/GREATEST(4-(2026-gc.entry_year),1) > 24 THEN 1 ELSE 0 END) AS high_now,
  SUM(CASE WHEN p.plan_total_credit - p.completed_credit > 0
        AND (p.plan_total_credit - p.completed_credit)/GREATEST(2*(4-(2026-gc.entry_year)),1) > 24 THEN 1 ELSE 0 END) AS high_fixed,
  COUNT(*) total
FROM stat_credit_progress p JOIN grade_cohort gc ON gc.id = p.grade_id
WHERE p.semester_id = 5
GROUP BY gc.grade_name, gc.entry_year ORDER BY gc.entry_year;

SELECT '=== E16 复核：sem5 零成绩班是否被判 FAIL_RATE_HIGH ===' AS section;
SELECT '有 SELECTED 且无 score 行的班数' AS metric, COUNT(*) v FROM teaching_class tc
WHERE tc.semester_id=5
  AND EXISTS (SELECT 1 FROM course_selection cs WHERE cs.teaching_class_id=tc.id AND cs.status='SELECTED')
  AND NOT EXISTS (SELECT 1 FROM course_selection cs JOIN score s ON s.course_selection_id=cs.id
                  WHERE cs.teaching_class_id=tc.id AND cs.status='SELECTED')
UNION ALL
SELECT '其中 739 类真实低及格率样本', 1;

SELECT '--- 各课程基线及格率（解释为何 0% 必然触发） ---' AS note;
SELECT 'course 数' AS metric, COUNT(*) v FROM course
UNION ALL SELECT '最小值/最大值见下', 0;

SELECT c.id, c.course_code, COUNT(*) n,
       ROUND(100*SUM(s.score_value>=60)/COUNT(*),2) AS course_pass_rate
FROM score s
JOIN course_selection cs ON cs.id=s.course_selection_id
JOIN teaching_class tc ON tc.id=cs.teaching_class_id
JOIN course c ON c.id=tc.course_id
GROUP BY c.id, c.course_code
ORDER BY course_pass_rate ASC LIMIT 5;
