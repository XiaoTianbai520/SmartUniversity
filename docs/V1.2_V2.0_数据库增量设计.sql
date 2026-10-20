-- ============================================================
-- 高校智慧教务选课平台 V1.2 + V2.0 数据库增量脚本
-- 前置：已执行
--   1) docs/高校智慧教务选课平台_V1_数据库建表.sql   （V1.0，17 张表）
--   2) docs/V1.1_数据库增量设计.sql                  （V1.1，+ notification 表）
-- 目标库：edu_course_selection
-- 适用版本：MySQL 8.x
-- 幂等性：表用 CREATE TABLE IF NOT EXISTS；列 / 索引 / CHECK 约束通过
--         information_schema 判断后再动态执行，可重复执行不报错。
-- 执行方式：使用 mysql 客户端执行本文件（脚本内使用了 DELIMITER，属客户端指令）
--   mysql -h 192.168.1.102 -u root -p edu_course_selection < V1.2_V2.0_数据库增量设计.sql
-- ============================================================

USE `edu_course_selection`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 0. 幂等辅助存储过程（脚本结束时自动删除）
-- ============================================================

DROP PROCEDURE IF EXISTS `sp_v12_add_column_if_absent`;
DROP PROCEDURE IF EXISTS `sp_v12_add_index_if_absent`;
DROP PROCEDURE IF EXISTS `sp_v12_drop_check_if_exists`;
DROP PROCEDURE IF EXISTS `sp_v12_add_check_if_absent`;

DELIMITER $$

CREATE PROCEDURE `sp_v12_add_column_if_absent`(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
    ) THEN
        SET @v12_sql = p_ddl;
        PREPARE v12_stmt FROM @v12_sql;
        EXECUTE v12_stmt;
        DEALLOCATE PREPARE v12_stmt;
    END IF;
END$$

CREATE PROCEDURE `sp_v12_add_index_if_absent`(
    IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index
    ) THEN
        SET @v12_sql = p_ddl;
        PREPARE v12_stmt FROM @v12_sql;
        EXECUTE v12_stmt;
        DEALLOCATE PREPARE v12_stmt;
    END IF;
END$$

CREATE PROCEDURE `sp_v12_drop_check_if_exists`(
    IN p_table VARCHAR(64), IN p_constraint VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND CONSTRAINT_NAME = p_constraint
    ) THEN
        SET @v12_sql = CONCAT('ALTER TABLE `', p_table, '` DROP CHECK `', p_constraint, '`');
        PREPARE v12_stmt FROM @v12_sql;
        EXECUTE v12_stmt;
        DEALLOCATE PREPARE v12_stmt;
    END IF;
END$$

CREATE PROCEDURE `sp_v12_add_check_if_absent`(
    IN p_table VARCHAR(64), IN p_constraint VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND CONSTRAINT_NAME = p_constraint
    ) THEN
        SET @v12_sql = p_ddl;
        PREPARE v12_stmt FROM @v12_sql;
        EXECUTE v12_stmt;
        DEALLOCATE PREPARE v12_stmt;
    END IF;
END$$

DELIMITER ;

-- ============================================================
-- 1. 教学班表：新增 selected_count（V1.2 并发写入互斥器）
-- ============================================================
-- 说明：
-- 1) selected_count 是 course_selection 中 status='SELECTED' 的冗余计数，
--    唯一用途是让事务内的「条件更新」成为防超卖的最终防线：
--      UPDATE teaching_class
--         SET selected_count = selected_count + 1
--       WHERE id = ? AND selected_count < ?
--    影响行数 = 1 才算抢到名额。
-- 2) 【红线】selected_count 仅用于并发写入互斥，任何统计 / 看板 / 对外接口
--    一律不得读取它。「已选人数」的权威口径恒为
--    COUNT(course_selection WHERE status='SELECTED')（PRD C1 决议）。
-- 3) 脚本执行后用子查询把存量数据初始化为真实值，避免上线即漂移。
-- 4) 后续由 CACHE_RECONCILE_TASK 每 30 分钟以 DB 为准校正。

CALL `sp_v12_add_column_if_absent`(
    'teaching_class', 'selected_count',
    'ALTER TABLE `teaching_class`
        ADD COLUMN `selected_count` INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT ''已选人数冗余计数，仅供并发写入条件更新使用，统计口径以 course_selection 为准''
        AFTER `capacity`');

UPDATE `teaching_class` tc
    SET tc.`selected_count` = (
        SELECT COUNT(*) FROM `course_selection` cs
        WHERE cs.`teaching_class_id` = tc.`id` AND cs.`status` = 'SELECTED')
WHERE EXISTS (
    SELECT 1 FROM `course_selection` cs2
    WHERE cs2.`teaching_class_id` = tc.`id` AND cs2.`status` = 'SELECTED');

-- 无任何选课记录的教学班保持 0，无需处理

-- ============================================================
-- 2. 培养方案表（V2.0 / S-01 / W3）
-- ============================================================
-- 说明：
-- 1) 绑定维度为「专业 + 入学年级」（PRD Q11 决议 A），
--    uk_training_plan_major_grade 保证同一组合唯一。
-- 2) status = 1 启用 / 0 停用，停用后不参与学生学分进度计算。
-- 3) 四类学分之和不得超过毕业总学分，由 CHECK 约束保证。
-- 4) 只做单版本 CRUD，不做版本管理与变更审计（PRD Q18）。

CREATE TABLE IF NOT EXISTS `training_plan` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '培养方案ID',
    `plan_name` VARCHAR(100) NOT NULL COMMENT '培养方案名称，例如：软件工程2026级培养方案',
    `major_id` BIGINT UNSIGNED NOT NULL COMMENT '所属专业ID',
    `grade_id` BIGINT UNSIGNED NOT NULL COMMENT '所属年级ID（入学年级）',
    `total_credit` DECIMAL(5,1) NOT NULL COMMENT '毕业总学分要求',
    `required_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '必修学分要求',
    `major_elective_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '专业选修学分要求',
    `public_elective_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '公共选修学分要求',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    `remark` VARCHAR(500) NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_plan_major_grade` (`major_id`, `grade_id`),
    KEY `idx_training_plan_major` (`major_id`),
    KEY `idx_training_plan_grade` (`grade_id`),
    KEY `idx_training_plan_status` (`status`),

    CONSTRAINT `fk_training_plan_major`
        FOREIGN KEY (`major_id`) REFERENCES `major` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_training_plan_grade`
        FOREIGN KEY (`grade_id`) REFERENCES `grade_cohort` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='培养方案表，专业 + 入学年级的毕业学分要求';

CALL `sp_v12_add_check_if_absent`(
    'training_plan', 'chk_training_plan_credit',
    'ALTER TABLE `training_plan`
        ADD CONSTRAINT `chk_training_plan_credit`
        CHECK (`total_credit` > 0
               AND `required_credit` >= 0
               AND `major_elective_credit` >= 0
               AND `public_elective_credit` >= 0
               AND (`required_credit` + `major_elective_credit` + `public_elective_credit`) <= `total_credit`)');

CALL `sp_v12_add_check_if_absent`(
    'training_plan', 'chk_training_plan_status',
    'ALTER TABLE `training_plan`
        ADD CONSTRAINT `chk_training_plan_status` CHECK (`status` IN (0, 1))');

-- ============================================================
-- 3. 教室利用率快照表（V2.0 / S-03 / W3）
-- ============================================================
-- 说明：按「学期 + 教室 + 快照日期」唯一，重复生成覆盖旧值（PRD Q12 决议 A）。

CREATE TABLE IF NOT EXISTS `stat_classroom_usage` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    `semester_id` BIGINT UNSIGNED NOT NULL COMMENT '学期ID',
    `classroom_id` BIGINT UNSIGNED NOT NULL COMMENT '教室ID',
    `building_name` VARCHAR(100) NOT NULL COMMENT '教学楼名称快照',
    `room_no` VARCHAR(30) NOT NULL COMMENT '教室编号快照',
    `capacity` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '教室容量快照',
    `usage_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '使用次数（排课条数）',
    `usage_hours` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '使用课时（节次×周数之和）',
    `total_hours` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '可排课时总数（分母）',
    `usage_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '利用率百分比，0~100，保留 2 位小数',
    `stat_date` DATE NOT NULL COMMENT '快照日期',
    `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_classroom` (`semester_id`, `classroom_id`, `stat_date`),
    KEY `idx_stat_classroom_semester` (`semester_id`),
    KEY `idx_stat_classroom_building` (`building_name`),
    KEY `idx_stat_classroom_rate` (`usage_rate`),

    CONSTRAINT `fk_stat_classroom_semester`
        FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_stat_classroom_classroom`
        FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教室利用率统计快照表';

-- ============================================================
-- 4. 教师工作量快照表（V2.0 / S-04 / W3）
-- ============================================================

CREATE TABLE IF NOT EXISTS `stat_teacher_workload` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    `semester_id` BIGINT UNSIGNED NOT NULL COMMENT '学期ID',
    `teacher_id` BIGINT UNSIGNED NOT NULL COMMENT '教师ID',
    `teacher_no` VARCHAR(30) NOT NULL COMMENT '教师编号快照',
    `teacher_name` VARCHAR(50) NOT NULL COMMENT '教师姓名快照',
    `class_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '教学班数',
    `total_hours` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总课时（节次×周数之和）',
    `student_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '学生总人数（去重后）',
    `stat_date` DATE NOT NULL COMMENT '快照日期',
    `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_teacher` (`semester_id`, `teacher_id`, `stat_date`),
    KEY `idx_stat_teacher_semester` (`semester_id`),
    KEY `idx_stat_teacher_teacher` (`teacher_id`),

    CONSTRAINT `fk_stat_teacher_semester`
        FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_stat_teacher_teacher`
        FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教师工作量统计快照表';

-- ============================================================
-- 5. 课程选课分析快照表（V2.0 / S-05 / W3）
-- ============================================================
-- 说明：selected_count 必须来自 COUNT(course_selection WHERE status='SELECTED')，
--       严禁写入 Redis 计数（PRD C1 红线）。

CREATE TABLE IF NOT EXISTS `stat_course_selection` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    `semester_id` BIGINT UNSIGNED NOT NULL COMMENT '学期ID',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `teaching_class_id` BIGINT UNSIGNED NOT NULL COMMENT '教学班ID',
    `course_code` VARCHAR(30) NOT NULL COMMENT '课程编号快照',
    `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称快照',
    `class_code` VARCHAR(50) NOT NULL COMMENT '教学班编号快照',
    `teacher_id` BIGINT UNSIGNED NULL COMMENT '任课教师ID',
    `teacher_name` VARCHAR(50) NULL COMMENT '任课教师姓名快照',
    `capacity` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '教学班容量',
    `selected_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已选人数（DB 权威口径，不含候补）',
    `waiting_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '候补人数',
    `withdraw_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '退课人数（仅 WITHDRAWN）',
    `full_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否满班：1满班（selected_count >= capacity），0未满',
    `stat_date` DATE NOT NULL COMMENT '快照日期',
    `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_course` (`semester_id`, `teaching_class_id`, `stat_date`),
    KEY `idx_stat_course_semester` (`semester_id`),
    KEY `idx_stat_course_course` (`course_id`),
    KEY `idx_stat_course_selected` (`selected_count`),
    KEY `idx_stat_course_full` (`full_flag`),

    CONSTRAINT `fk_stat_course_semester`
        FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_stat_course_course`
        FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_stat_course_class`
        FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='课程选课分析统计快照表';

-- ============================================================
-- 6. 学生学分进度快照表（V2.0 / S-02 + I-01 / W3）
-- ============================================================
-- 说明：
-- 1) has_plan = 0 表示「专业 + 年级无对应培养方案」，此时各项要求为 0，
--    接口需返回明确提示而非报错。
-- 2) 【决策 1 已拍板 / PRD Q10-B】已完成学分口径：score.status = 'PUBLISHED' 即计入，
--    不判及格（与 V1.1 CreditServiceImpl 行为一致）；
--    passed_credit 为附加的「及格学分」字段（score_value >= 60），
--    仅供展示，不参与 progress_rate 与 risk_level 计算。
-- 3) 【C8 硬约束】本表的 completed_credit / passed_credit 一律为【跨学期累计】：
--    禁止复用 V1.1 的 CreditServiceImpl#getCreditSummary、
--    ScoreMapper#listPublishedScoreByStudent（内部 JOIN teaching_class WHERE semester_id = ?）
--    或任何带 semester_id 过滤的学分方法。semester_id 列仅表示「快照所属学期」，
--    不代表学分统计范围。
-- 4) risk_level：LOW / MEDIUM / HIGH。

CREATE TABLE IF NOT EXISTS `stat_credit_progress` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    `semester_id` BIGINT UNSIGNED NOT NULL COMMENT '学期ID',
    `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `student_no` VARCHAR(30) NOT NULL COMMENT '学号快照',
    `student_name` VARCHAR(50) NOT NULL COMMENT '学生姓名快照',
    `major_id` BIGINT UNSIGNED NULL COMMENT '所属专业ID快照',
    `grade_id` BIGINT UNSIGNED NULL COMMENT '所属年级ID快照',
    `has_plan` TINYINT NOT NULL DEFAULT 0 COMMENT '是否存在培养方案：1有，0无',
    `completed_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '已完成总学分（跨学期累计，已发布即计入，不判及格）',
    `passed_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '及格学分（跨学期累计，score_value>=60，附加展示字段，不参与完成度与风险计算）',
    `required_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '已完成的必修学分',
    `major_elective_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '已完成的专业选修学分',
    `public_elective_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '已完成的公共选修学分',
    `plan_total_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '培养方案毕业总学分要求',
    `plan_required_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '培养方案必修学分要求',
    `plan_major_elective_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '培养方案专业选修学分要求',
    `plan_public_elective_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '培养方案公共选修学分要求',
    `progress_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '总体完成度百分比，0~100',
    `risk_level` VARCHAR(10) NOT NULL DEFAULT 'LOW' COMMENT '毕业风险等级：LOW/MEDIUM/HIGH',
    `stat_date` DATE NOT NULL COMMENT '快照日期',
    `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_credit` (`semester_id`, `student_id`, `stat_date`),
    KEY `idx_stat_credit_semester` (`semester_id`),
    KEY `idx_stat_credit_major_grade` (`major_id`, `grade_id`),
    KEY `idx_stat_credit_risk` (`risk_level`),
    KEY `idx_stat_credit_rate` (`progress_rate`),

    CONSTRAINT `fk_stat_credit_semester`
        FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_stat_credit_student`
        FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_stat_credit_risk_level`
        CHECK (`risk_level` IN ('LOW', 'MEDIUM', 'HIGH')),

    CONSTRAINT `chk_stat_credit_has_plan`
        CHECK (`has_plan` IN (0, 1)),

    CONSTRAINT `chk_stat_credit_rate`
        CHECK (`progress_rate` BETWEEN 0 AND 100)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学生学分进度统计快照表';

-- 幂等补列：若本表已由旧版脚本建过，补齐 passed_credit 列（决策 1 新增的附加字段）
CALL `sp_v12_add_column_if_absent`(
    'stat_credit_progress', 'passed_credit',
    'ALTER TABLE `stat_credit_progress`
        ADD COLUMN `passed_credit` DECIMAL(5,1) NOT NULL DEFAULT 0.0
        COMMENT ''及格学分（跨学期累计，score_value>=60，附加展示字段，不参与完成度与风险计算）''
        AFTER `completed_credit`');

-- ============================================================
-- 7. 教务看板指标快照表（V2.0 / S-06 / W3）
-- ============================================================
-- 说明：按「学期 + 快照日期」唯一，字段与 PRD §7.2 看板指标一一对应。
--       batch_status 取 selection_batch.status，无批次时为 NOT_STARTED。

CREATE TABLE IF NOT EXISTS `stat_semester_overview` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    `semester_id` BIGINT UNSIGNED NOT NULL COMMENT '学期ID',
    `semester_code` VARCHAR(30) NOT NULL COMMENT '学期编号快照',
    `student_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '在册学生数（COUNT(student WHERE status=1)）',
    `course_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本学期开设课程数（DISTINCT course_id）',
    `teaching_class_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本学期教学班数',
    `teacher_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本学期任课教师数（DISTINCT teacher_id）',
    `selection_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前选课人次（status=SELECTED）',
    `waitlist_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前候补人次（status=WAITING）',
    `withdraw_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本学期退课人次（status=WITHDRAWN）',
    `full_class_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '课程满班率百分比，0~100',
    `classroom_usage_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '教室利用率百分比，0~100',
    `avg_teacher_workload` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '教师平均工作量（课时）',
    `avg_credit_progress_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '平均学分完成度百分比',
    `unpublished_class_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '待发布成绩教学班数',
    `batch_status` VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
        COMMENT '当前选课批次状态：NOT_STARTED/IN_PROGRESS/ENDED',
    `stat_date` DATE NOT NULL COMMENT '快照日期',
    `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间，作为接口 snapshotTime',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_overview` (`semester_id`, `stat_date`),
    KEY `idx_stat_overview_semester` (`semester_id`),
    KEY `idx_stat_overview_generated` (`generated_at`),

    CONSTRAINT `fk_stat_overview_semester`
        FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_stat_overview_batch_status`
        CHECK (`batch_status` IN ('NOT_STARTED', 'IN_PROGRESS', 'ENDED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教务数据看板指标快照表';

-- ============================================================
-- 8. 定时任务执行日志表（公共 / A-07 / T0）
-- ============================================================
-- 说明：
-- 1) 每轮执行落一条；未抢到分布式锁直接跳过，不落库，避免日志表膨胀。
-- 2) instance_id 用于多实例部署时定位执行机器。
-- 3) 本表不做物理删除，长期运行后可按 created_at 归档清理。

CREATE TABLE IF NOT EXISTS `schedule_task_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `task_code` VARCHAR(40) NOT NULL COMMENT '任务编码，见 TaskCodeEnum',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务中文名',
    `trigger_type` VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT '触发方式：SCHEDULED/MANUAL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态：RUNNING/SUCCESS/FAILED/SKIPPED',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NULL COMMENT '结束时间',
    `duration_ms` BIGINT UNSIGNED NULL COMMENT '耗时（毫秒）',
    `scan_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '扫描条数',
    `success_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '成功条数',
    `fail_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失败条数',
    `error_summary` VARCHAR(1000) NULL COMMENT '异常摘要',
    `instance_id` VARCHAR(100) NULL COMMENT '执行实例标识（主机名:端口）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_task_log_code` (`task_code`),
    KEY `idx_task_log_code_start` (`task_code`, `start_time`),
    KEY `idx_task_log_status` (`status`),
    KEY `idx_task_log_created` (`created_at`),

    CONSTRAINT `chk_task_log_trigger_type`
        CHECK (`trigger_type` IN ('SCHEDULED', 'MANUAL')),

    CONSTRAINT `chk_task_log_status`
        CHECK (`status` IN ('RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='定时任务执行日志表';

-- ============================================================
-- 9. 异常数据巡检结果表（V2.0 / A-08 / W4）
-- ============================================================
-- 说明：只读结果表，巡检任务不改任何业务数据；按「规则编码 + 业务ID」覆盖写。

CREATE TABLE IF NOT EXISTS `data_audit_issue` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '问题ID',
    `rule_code` VARCHAR(40) NOT NULL COMMENT '巡检规则编码，例如 CAPACITY_ZERO / SCHEDULE_CONFLICT',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则中文名',
    `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型：TEACHING_CLASS/CLASSROOM/TEACHER/SCORE/SEMESTER',
    `biz_id` BIGINT UNSIGNED NULL COMMENT '关联业务ID',
    `severity` VARCHAR(10) NOT NULL DEFAULT 'LOW' COMMENT '严重级别：LOW/MEDIUM/HIGH',
    `description` VARCHAR(500) NOT NULL COMMENT '问题描述',
    `semester_id` BIGINT UNSIGNED NULL COMMENT '所属学期ID，可为空',
    `stat_date` DATE NOT NULL COMMENT '巡检日期',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audit_issue` (`rule_code`, `biz_type`, `biz_id`),
    KEY `idx_audit_issue_semester` (`semester_id`),
    KEY `idx_audit_issue_severity` (`severity`),
    KEY `idx_audit_issue_rule` (`rule_code`),
    KEY `idx_audit_issue_date` (`stat_date`),

    CONSTRAINT `chk_audit_issue_severity`
        CHECK (`severity` IN ('LOW', 'MEDIUM', 'HIGH'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='异常数据巡检结果表，只读不改业务数据';

-- ============================================================
-- 10. 学期归档表（V2.0 / A-09 / W4）
-- ============================================================
-- 说明：归档内容为 JSON 汇总结果（选课与成绩聚合），按「学期 + 归档类型」覆盖写，幂等。

CREATE TABLE IF NOT EXISTS `semester_archive` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '归档ID',
    `semester_id` BIGINT UNSIGNED NOT NULL COMMENT '学期ID',
    `archive_type` VARCHAR(30) NOT NULL COMMENT '归档类型：SELECTION/SCORE/OVERVIEW',
    `archive_content` JSON NULL COMMENT '归档内容（JSON）',
    `archived_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_archive` (`semester_id`, `archive_type`),
    KEY `idx_semester_archive_semester` (`semester_id`),

    CONSTRAINT `fk_semester_archive_semester`
        FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_semester_archive_type`
        CHECK (`archive_type` IN ('SELECTION', 'SCORE', 'OVERVIEW'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学期末数据归档表';

-- ============================================================
-- 11. 通知表：新增 V2.0 通知类型（CREDIT_WARNING / SCORE_ANOMALY）
-- ============================================================
-- 说明：
-- 1) NoticeTypeEnum 同步新增这两个枚举（T0 交付）。
-- 2) 幂等键仍为 uk_notice_dedup(notice_type, biz_id, receiver_user_id)。
--    【决策 2 已拍板】「同一学生同一学期只预警一次」通过 biz_id 直接存 semester_id 实现：
--        biz_id = semester_id，学生维度由 receiver_user_id（sys_user.id）承担。
--    ❗已废弃原「biz_id = semester_id * 100000000 + student_id」编码方案
--      （见架构文档 §12.3 / R-05，接口清单 42.1 同步修订），本表不新增任何列。

CALL `sp_v12_drop_check_if_exists`('notification', 'chk_notice_type');

CALL `sp_v12_add_check_if_absent`(
    'notification', 'chk_notice_type',
    'ALTER TABLE `notification`
        ADD CONSTRAINT `chk_notice_type`
        CHECK (`notice_type` IN (''SELECTION_START'', ''SELECTION_END'', ''WAITLIST_PROMOTED'',
                                 ''COURSE_ADJUSTED'', ''SCORE_PUBLISHED'',
                                 ''CREDIT_WARNING'', ''SCORE_ANOMALY''))');

-- ============================================================
-- 12. 存量表索引补齐（支撑 V1.2 对账与 V2.0 统计）
-- ============================================================

-- 选课记录：按批次 + 状态统计（看板 selectionCount / waitlistCount / withdrawCount）
CALL `sp_v12_add_index_if_absent`(
    'course_selection', 'idx_selection_batch_status',
    'ALTER TABLE `course_selection` ADD KEY `idx_selection_batch_status` (`batch_id`, `status`)');

-- 教学班：按学期 + 教师聚合（教师工作量）
CALL `sp_v12_add_index_if_absent`(
    'teaching_class', 'idx_tc_semester_teacher',
    'ALTER TABLE `teaching_class` ADD KEY `idx_tc_semester_teacher` (`semester_id`, `teacher_id`)');

-- 教学班：按学期 + 状态 + 教师联合过滤（看板 teacherCount + 工作量）
CALL `sp_v12_add_index_if_absent`(
    'teaching_class', 'idx_tc_semester_status_teacher',
    'ALTER TABLE `teaching_class` ADD KEY `idx_tc_semester_status_teacher` (`semester_id`, `status`, `teacher_id`)');

-- 排课：按星期 + 节次（教室空闲时段计算）
CALL `sp_v12_add_index_if_absent`(
    'teaching_class_schedule', 'idx_schedule_weekday_section',
    'ALTER TABLE `teaching_class_schedule`
        ADD KEY `idx_schedule_weekday_section` (`weekday`, `start_section`, `end_section`)');

-- 教室：按教学楼汇总利用率
CALL `sp_v12_add_index_if_absent`(
    'classroom', 'idx_classroom_building',
    'ALTER TABLE `classroom` ADD KEY `idx_classroom_building` (`building_name`)');

-- 教室：按状态统计可用教室数（利用率分母）
CALL `sp_v12_add_index_if_absent`(
    'classroom', 'idx_classroom_status',
    'ALTER TABLE `classroom` ADD KEY `idx_classroom_status` (`status`)');

-- 学生：按状态统计在册学生数（看板 studentCount）
CALL `sp_v12_add_index_if_absent`(
    'student', 'idx_student_status',
    'ALTER TABLE `student` ADD KEY `idx_student_status` (`status`)');

-- 成绩：按状态 + 分数（学分累计的已发布判定 / 附加及格学分字段、成绩异常基线）
--       注：决策 1 已拍板「发布即计入」，本索引主要服务 passed_credit 及格过滤与异常基线
CALL `sp_v12_add_index_if_absent`(
    'score', 'idx_score_status_value',
    'ALTER TABLE `score` ADD KEY `idx_score_status_value` (`status`, `score_value`)');

-- 成绩：按状态 + 发布时间（成绩发布窗口巡检）
CALL `sp_v12_add_index_if_absent`(
    'score', 'idx_score_published_at',
    'ALTER TABLE `score` ADD KEY `idx_score_published_at` (`status`, `published_at`)');

-- 选课批次：按状态 + 时间（批次自动开启 / 关闭扫描）
CALL `sp_v12_add_index_if_absent`(
    'selection_batch', 'idx_batch_status_time',
    'ALTER TABLE `selection_batch` ADD KEY `idx_batch_status_time` (`status`, `start_time`, `end_time`)');

-- ============================================================
-- 13. 清理辅助存储过程
-- ============================================================

DROP PROCEDURE IF EXISTS `sp_v12_add_column_if_absent`;
DROP PROCEDURE IF EXISTS `sp_v12_add_index_if_absent`;
DROP PROCEDURE IF EXISTS `sp_v12_drop_check_if_exists`;
DROP PROCEDURE IF EXISTS `sp_v12_add_check_if_absent`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- V1.2 + V2.0 数据库设计说明
-- ============================================================
--
-- 1. 本次新增 9 张表：
--      training_plan            培养方案（V2.0 / S-01）
--      stat_classroom_usage     教室利用率快照（V2.0 / S-03）
--      stat_teacher_workload    教师工作量快照（V2.0 / S-04）
--      stat_course_selection    课程选课分析快照（V2.0 / S-05）
--      stat_credit_progress     学生学分进度快照（V2.0 / S-02 + I-01）
--      stat_semester_overview   教务看板指标快照（V2.0 / S-06）
--      schedule_task_log        定时任务执行日志（公共 / A-07）
--      data_audit_issue         异常数据巡检结果（V2.0 / A-08）
--      semester_archive         学期归档（V2.0 / A-09）
--    存量表改造 2 项：teaching_class 新增 selected_count；
--                    notification 的 chk_notice_type 重建。
--
-- 2. 【红线一】「已选人数」的权威口径恒为
--    COUNT(course_selection WHERE teaching_class_id = ? AND status = 'SELECTED')。
--    teaching_class.selected_count 仅为并发写入的条件更新互斥器，
--    Redis 计数 Key 仅为预扣减加速层，二者都不得作为任何统计数据源（PRD C1）。
--
-- 3. 【红线二】所有 stat_* 快照表按「学期 + 业务ID + 快照日期」唯一，
--    重复生成覆盖旧值（PRD Q12 决议 A：每学期只保留最新一份），
--    由 STAT_SNAPSHOT_TASK 每日 02:00 错峰刷新，单次扫描上限 500 条（PRD C3）。
--
-- 4. 主键全部自增，无逻辑删除字段；启停统一用 status / 语义字段表达，
--    与 V1.0 / V1.1 现有规范一致。外键统一 ON UPDATE CASCADE ON DELETE RESTRICT。
--
-- 5. 幂等说明：本脚本可重复执行。表用 CREATE TABLE IF NOT EXISTS，
--    列 / 索引 / CHECK 约束通过 information_schema 判断后再动态执行。
--    唯一会重复执行的写操作是 selected_count 的存量初始化 UPDATE，
--    该语句以 DB 真实计数为准，重复执行结果一致（幂等）。
--
-- 6. 执行顺序：本脚本必须在 V1.0 建表脚本与 V1.1 增量脚本之后执行。
--
-- 7. 【I-05 已砍除】本脚本中【不存在】任何与个性化课程推荐相关的表 / 列 / 索引
--    （原计划无新增表，故无需删除任何对象）。接口 42.4 与 CourseRecommendService 一并删除，
--    编号回收不递补。V2.0 需求条数 20 → 19，新增接口 28 → 27。
--
-- 8. 【决策 3 / Q19 已拍板】不给 semester 表补「学期周数」字段：
--    教室利用率分母中的学期周数由 LEAST(CEIL(DATEDIFF(semester.end_date, semester.start_date)/7), maxWeeks)
--    推导，上限参数 smart-university.statistics.classroom-usage.max-weeks-per-semester 默认 20 且可配。
--    本脚本【不包含】semester 的任何 ALTER。
--
-- 9. 【C8 硬约束】学分累计口径：stat_credit_progress.completed_credit / passed_credit 为跨学期累计，
--    禁止复用 V1.1 的 CreditServiceImpl 与 ScoreMapper#listPublishedScoreByStudent（带 semester_id 过滤）。
--    本脚本不新增学分明细表，累计结果由 ScoreMapper#listPublishedScoreByStudentId（新增，无学期过滤）
--    与 CourseSelectionMapper#listSelectionByStudentId（新增）在 Service 层聚合后写入本表。
-- ============================================================
