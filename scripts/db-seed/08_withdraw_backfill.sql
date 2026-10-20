-- =====================================================================
-- 08_withdraw_backfill.sql
-- 目标：sem3(2025-2026-1, id=3) / sem4(2025-2026-2, id=4) 退课率补齐到 ~6.5%
-- 规则：只抽 SELECTED；排除专项样本班；退课同时删除对应 score；重算 selected_count
-- 幂等：以 status='SELECTED' 为抽取池，重复执行只会再次从剩余 SELECTED 中抽取
-- =====================================================================
SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_wd;
CREATE TEMPORARY TABLE tmp_wd (
  id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  semester_id BIGINT UNSIGNED NOT NULL
);

-- sem3：抽 620 条
INSERT INTO tmp_wd (id, semester_id)
SELECT cs.id, tc.semester_id
FROM course_selection cs
JOIN teaching_class tc ON tc.id = cs.teaching_class_id
WHERE tc.semester_id = 3
  AND cs.status = 'SELECTED'
  AND tc.id NOT IN (737, 738, 740, 800, 803, 817, 857, 935, 974)
ORDER BY RAND()
LIMIT 620;

-- sem4：抽 780 条
INSERT INTO tmp_wd (id, semester_id)
SELECT cs.id, tc.semester_id
FROM course_selection cs
JOIN teaching_class tc ON tc.id = cs.teaching_class_id
WHERE tc.semester_id = 4
  AND cs.status = 'SELECTED'
  AND tc.id NOT IN (737, 738, 740, 800, 803, 817, 857, 935, 974)
ORDER BY RAND()
LIMIT 780;

-- 删除退课记录对应的成绩（退课不应有成绩）
DELETE sc FROM score sc JOIN tmp_wd t ON t.id = sc.course_selection_id;

-- 置为 WITHDRAWN，withdrawn_at 落在 [selected_at, drop_deadline] 随机时刻
UPDATE course_selection cs
JOIN tmp_wd t ON t.id = cs.id
JOIN selection_batch b ON b.id = cs.batch_id
SET cs.status = 'WITHDRAWN',
    cs.withdrawn_at = TIMESTAMPADD(
        SECOND,
        FLOOR(RAND() * GREATEST(TIMESTAMPDIFF(SECOND, cs.selected_at, b.drop_deadline), 60)),
        cs.selected_at),
    cs.updated_at = NOW();
