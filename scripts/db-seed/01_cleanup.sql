-- =====================================================================
-- 01_cleanup.sql  —— 任务#1：清理脏数据 + 修正学期/批次结构 + 固定账号改名
-- 幂等：可重复执行（全部 DELETE / INSERT ... WHERE NOT EXISTS）
-- 执行：mysql -h.. -D edu_course_selection --default-character-set=utf8mb4 < 01_cleanup.sql
-- =====================================================================
SET NAMES utf8mb4;
SET SESSION sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

-- ---------------------------------------------------------------------
-- Step A. 先清空派生/快照类表（它们对 semester / teaching_class 有 RESTRICT 外键，
--         必须在删除学期与教学班之前清空），并删除 semester=8 的归档
-- ---------------------------------------------------------------------
TRUNCATE TABLE stat_semester_overview;
TRUNCATE TABLE stat_course_selection;
TRUNCATE TABLE stat_credit_progress;
TRUNCATE TABLE stat_teacher_workload;
TRUNCATE TABLE stat_classroom_usage;
TRUNCATE TABLE data_audit_issue;
DELETE FROM semester_archive WHERE semester_id = 8;

-- ---------------------------------------------------------------------
-- Step B. 清理脏教学班 id 1107~1112 及其关联的 成绩 / 选课 / 批次关联 / 通知
--   1107 = 2027Java(sem7)  1108~1111 = sem8 脏班  1112 = c011(sem6)
-- ---------------------------------------------------------------------
DELETE sc FROM score sc
  JOIN course_selection cs ON cs.id = sc.course_selection_id
 WHERE cs.teaching_class_id BETWEEN 1107 AND 1112;

DELETE FROM score
 WHERE course_selection_id IN (
   SELECT id FROM course_selection WHERE student_id = 2001
 );

DELETE FROM course_selection
 WHERE teaching_class_id BETWEEN 1107 AND 1112
    OR student_id = 2001;

DELETE FROM selection_batch_class WHERE teaching_class_id BETWEEN 1107 AND 1112;

DELETE FROM notification
 WHERE biz_id IN (6, 7, 8, 166, 167, 168, 1107, 1108, 1109, 1110, 1111, 1112);

DELETE FROM teaching_class_schedule WHERE teaching_class_id BETWEEN 1107 AND 1112;
DELETE FROM teaching_class_major     WHERE teaching_class_id BETWEEN 1107 AND 1112;
DELETE FROM teaching_class_grade     WHERE teaching_class_id BETWEEN 1107 AND 1112;
DELETE FROM teaching_class           WHERE id BETWEEN 1107 AND 1112;

-- ---------------------------------------------------------------------
-- Step C. 删除脏批次 6 / 7(1111) / 8
-- ---------------------------------------------------------------------
DELETE FROM selection_batch WHERE id IN (6, 7, 8);

-- ---------------------------------------------------------------------
-- Step D. 删除脏课程 165~168
-- ---------------------------------------------------------------------
DELETE FROM course WHERE id BETWEEN 165 AND 168;

-- ---------------------------------------------------------------------
-- Step E. 删除脏学生 2001 / 11111 与其系统账号 2182
-- ---------------------------------------------------------------------
DELETE FROM student  WHERE id = 2001;
DELETE FROM sys_user WHERE id = 2182;

-- ---------------------------------------------------------------------
-- Step F. 删除脏学期 8 (111111)
-- ---------------------------------------------------------------------
DELETE FROM semester WHERE id = 8;

-- ---------------------------------------------------------------------
-- Step G. 修正学期 6 / 7 的日期与状态
--   6 = 2026-2027-2 应为 PLANNED、日期在今天之后
--   7 = 2027-2028-1 应为 PLANNED、起始日期 2027-09-01
-- ---------------------------------------------------------------------
UPDATE semester
   SET start_date = '2027-02-22', end_date = '2027-07-10', status = 'PLANNED'
 WHERE semester_code = '2026-2027-2';

UPDATE semester
   SET start_date = '2027-09-01', end_date = '2028-01-20', status = 'PLANNED',
       max_selection_credit = 30.0
 WHERE semester_code = '2027-2028-1';

-- ---------------------------------------------------------------------
-- Step H. 新增 4 个历史学期（2022-2023-1 .. 2023-2024-2），status=FINISHED
-- ---------------------------------------------------------------------
INSERT INTO semester (semester_code, academic_year, term_no, start_date, end_date,
                      max_selection_credit, status)
SELECT '2022-2023-1', '2022-2023', 1, '2022-09-05', '2023-01-13', 30.0, 'FINISHED'
 WHERE NOT EXISTS (SELECT 1 FROM semester WHERE semester_code = '2022-2023-1');

INSERT INTO semester (semester_code, academic_year, term_no, start_date, end_date,
                      max_selection_credit, status)
SELECT '2022-2023-2', '2022-2023', 2, '2023-02-20', '2023-07-07', 30.0, 'FINISHED'
 WHERE NOT EXISTS (SELECT 1 FROM semester WHERE semester_code = '2022-2023-2');

INSERT INTO semester (semester_code, academic_year, term_no, start_date, end_date,
                      max_selection_credit, status)
SELECT '2023-2024-1', '2023-2024', 1, '2023-09-04', '2024-01-19', 30.0, 'FINISHED'
 WHERE NOT EXISTS (SELECT 1 FROM semester WHERE semester_code = '2023-2024-1');

INSERT INTO semester (semester_code, academic_year, term_no, start_date, end_date,
                      max_selection_credit, status)
SELECT '2023-2024-2', '2023-2024', 2, '2024-02-26', '2024-07-12', 30.0, 'FINISHED'
 WHERE NOT EXISTS (SELECT 1 FROM semester WHERE semester_code = '2023-2024-2');

-- ---------------------------------------------------------------------
-- Step I. 为 4 个新学期各建 1 条选课批次
--   开始 = 学期开始后第 2 周 08:00；结束 = 学期结束前 10 天 23:59:59
-- ---------------------------------------------------------------------
INSERT INTO selection_batch (batch_name, semester_id, start_time, end_time, drop_deadline, status)
SELECT '2022-2023-1 选课批次', s.id, '2022-09-19 08:00:00', '2023-01-03 23:59:59', '2023-01-03 23:59:59', 'ENDED'
  FROM semester s
 WHERE s.semester_code = '2022-2023-1'
   AND NOT EXISTS (SELECT 1 FROM selection_batch b WHERE b.batch_name = '2022-2023-1 选课批次');

INSERT INTO selection_batch (batch_name, semester_id, start_time, end_time, drop_deadline, status)
SELECT '2022-2023-2 选课批次', s.id, '2023-03-06 08:00:00', '2023-06-27 23:59:59', '2023-06-27 23:59:59', 'ENDED'
  FROM semester s
 WHERE s.semester_code = '2022-2023-2'
   AND NOT EXISTS (SELECT 1 FROM selection_batch b WHERE b.batch_name = '2022-2023-2 选课批次');

INSERT INTO selection_batch (batch_name, semester_id, start_time, end_time, drop_deadline, status)
SELECT '2023-2024-1 选课批次', s.id, '2023-09-18 08:00:00', '2024-01-09 23:59:59', '2024-01-09 23:59:59', 'ENDED'
  FROM semester s
 WHERE s.semester_code = '2023-2024-1'
   AND NOT EXISTS (SELECT 1 FROM selection_batch b WHERE b.batch_name = '2023-2024-1 选课批次');

INSERT INTO selection_batch (batch_name, semester_id, start_time, end_time, drop_deadline, status)
SELECT '2023-2024-2 选课批次', s.id, '2024-03-11 08:00:00', '2024-07-02 23:59:59', '2024-07-02 23:59:59', 'ENDED'
  FROM semester s
 WHERE s.semester_code = '2023-2024-2'
   AND NOT EXISTS (SELECT 1 FROM selection_batch b WHERE b.batch_name = '2023-2024-2 选课批次');

-- ---------------------------------------------------------------------
-- Step J. 固定测试账号改名（用户名/密码不动）
-- ---------------------------------------------------------------------
UPDATE student SET student_name = '张明远' WHERE student_no = '20260001';
UPDATE student SET student_name = '李思远' WHERE student_no = '20260002';
UPDATE student SET student_name = '王雨桐' WHERE student_no = '20260003';
UPDATE teacher SET teacher_name = '张启明' WHERE id = 1;
