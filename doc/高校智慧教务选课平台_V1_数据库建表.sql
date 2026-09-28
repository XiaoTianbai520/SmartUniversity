-- ============================================================
-- 高校智慧教务选课平台 V1
-- MySQL 8.x 建表脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS `edu_course_selection`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `edu_course_selection`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 清理旧表（按外键依赖逆序）
-- ============================================================

DROP TABLE IF EXISTS `score`;
DROP TABLE IF EXISTS `course_selection`;
DROP TABLE IF EXISTS `selection_batch_class`;
DROP TABLE IF EXISTS `selection_batch`;
DROP TABLE IF EXISTS `teaching_class_schedule`;
DROP TABLE IF EXISTS `teaching_class_grade`;
DROP TABLE IF EXISTS `teaching_class_major`;
DROP TABLE IF EXISTS `teaching_class`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `semester`;
DROP TABLE IF EXISTS `classroom`;
DROP TABLE IF EXISTS `teacher`;
DROP TABLE IF EXISTS `student`;
DROP TABLE IF EXISTS `academic_class`;
DROP TABLE IF EXISTS `grade_cohort`;
DROP TABLE IF EXISTS `major`;
DROP TABLE IF EXISTS `sys_user`;

-- ============================================================
-- 1. 系统用户表
-- ============================================================

CREATE TABLE `sys_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希，不允许明文保存',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1启用，0禁用',
    `last_login_at` DATETIME NULL COMMENT '最后登录时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),

    CONSTRAINT `chk_sys_user_role`
        CHECK (`role` IN ('STUDENT', 'TEACHER', 'ADMIN')),
    CONSTRAINT `chk_sys_user_status`
        CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表';

-- ============================================================
-- 2. 专业表
-- ============================================================

CREATE TABLE `major` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '专业ID',
    `major_code` VARCHAR(30) NOT NULL COMMENT '专业编号',
    `major_name` VARCHAR(100) NOT NULL COMMENT '专业名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_major_code` (`major_code`),

    CONSTRAINT `chk_major_status`
        CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='专业表';

-- ============================================================
-- 3. 年级表
-- ============================================================

CREATE TABLE `grade_cohort` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '年级ID',
    `grade_name` VARCHAR(30) NOT NULL COMMENT '年级名称，例如2026级',
    `entry_year` SMALLINT UNSIGNED NOT NULL COMMENT '入学年份',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_grade_name` (`grade_name`),
    UNIQUE KEY `uk_grade_entry_year` (`entry_year`),

    CONSTRAINT `chk_grade_status`
        CHECK (`status` IN (0, 1)),
    CONSTRAINT `chk_grade_entry_year`
        CHECK (`entry_year` BETWEEN 2000 AND 2200)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='年级表';

-- ============================================================
-- 4. 行政班级表
-- ============================================================

CREATE TABLE `academic_class` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `class_code` VARCHAR(30) NOT NULL COMMENT '班级编号',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称',
    `major_id` BIGINT UNSIGNED NOT NULL COMMENT '所属专业ID',
    `grade_id` BIGINT UNSIGNED NOT NULL COMMENT '所属年级ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_academic_class_code` (`class_code`),
    KEY `idx_academic_class_major` (`major_id`),
    KEY `idx_academic_class_grade` (`grade_id`),

    CONSTRAINT `fk_academic_class_major`
        FOREIGN KEY (`major_id`) REFERENCES `major` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_academic_class_grade`
        FOREIGN KEY (`grade_id`) REFERENCES `grade_cohort` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_academic_class_status`
        CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='行政班级表';

-- ============================================================
-- 5. 学生表
-- ============================================================

CREATE TABLE `student` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学生ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '对应系统用户ID',
    `student_no` VARCHAR(30) NOT NULL COMMENT '学号',
    `student_name` VARCHAR(50) NOT NULL COMMENT '学生姓名',
    `major_id` BIGINT UNSIGNED NOT NULL COMMENT '所属专业ID',
    `grade_id` BIGINT UNSIGNED NOT NULL COMMENT '所属年级ID',
    `class_id` BIGINT UNSIGNED NOT NULL COMMENT '所属行政班级ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '学生状态：1正常，0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_user` (`user_id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_student_major` (`major_id`),
    KEY `idx_student_grade` (`grade_id`),
    KEY `idx_student_class` (`class_id`),

    CONSTRAINT `fk_student_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_student_major`
        FOREIGN KEY (`major_id`) REFERENCES `major` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_student_grade`
        FOREIGN KEY (`grade_id`) REFERENCES `grade_cohort` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_student_class`
        FOREIGN KEY (`class_id`) REFERENCES `academic_class` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_student_status`
        CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学生表';

-- ============================================================
-- 6. 教师表
-- ============================================================

CREATE TABLE `teacher` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教师ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '对应系统用户ID',
    `teacher_no` VARCHAR(30) NOT NULL COMMENT '教师编号',
    `teacher_name` VARCHAR(50) NOT NULL COMMENT '教师姓名',
    `title` VARCHAR(50) NULL COMMENT '职称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '教师状态：1正常，0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_user` (`user_id`),
    UNIQUE KEY `uk_teacher_no` (`teacher_no`),

    CONSTRAINT `fk_teacher_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_teacher_status`
        CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教师表';

-- ============================================================
-- 7. 教室表
-- ============================================================

CREATE TABLE `classroom` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教室ID',
    `building_name` VARCHAR(100) NOT NULL COMMENT '教学楼名称',
    `room_no` VARCHAR(30) NOT NULL COMMENT '教室编号',
    `capacity` INT UNSIGNED NOT NULL COMMENT '教室容量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1可用，0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_classroom_building_room` (`building_name`, `room_no`),

    CONSTRAINT `chk_classroom_capacity`
        CHECK (`capacity` > 0),
    CONSTRAINT `chk_classroom_status`
        CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教室表';

-- ============================================================
-- 8. 学期表
-- ============================================================

CREATE TABLE `semester` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学期ID',
    `semester_code` VARCHAR(30) NOT NULL COMMENT '学期编号，例如2026-2027-1',
    `academic_year` VARCHAR(20) NOT NULL COMMENT '学年，例如2026-2027',
    `term_no` TINYINT UNSIGNED NOT NULL COMMENT '学期序号',
    `start_date` DATE NOT NULL COMMENT '学期开始日期',
    `end_date` DATE NOT NULL COMMENT '学期结束日期',
    `max_selection_credit` DECIMAL(5,1) NOT NULL DEFAULT 30.0 COMMENT '学生本学期最大可选学分',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/ACTIVE/FINISHED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_code` (`semester_code`),
    KEY `idx_semester_status` (`status`),

    CONSTRAINT `chk_semester_term_no`
        CHECK (`term_no` BETWEEN 1 AND 3),
    CONSTRAINT `chk_semester_date`
        CHECK (`start_date` < `end_date`),
    CONSTRAINT `chk_semester_credit`
        CHECK (`max_selection_credit` > 0),
    CONSTRAINT `chk_semester_status`
        CHECK (`status` IN ('PLANNED', 'ACTIVE', 'FINISHED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学期表';

-- ============================================================
-- 9. 课程表
-- ============================================================

CREATE TABLE `course` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课程ID',
    `course_code` VARCHAR(30) NOT NULL COMMENT '课程编号',
    `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `description` TEXT NULL COMMENT '课程简介',
    `credit` DECIMAL(4,1) NOT NULL COMMENT '课程学分',
    `course_type` VARCHAR(30) NOT NULL COMMENT 'REQUIRED/MAJOR_ELECTIVE/PUBLIC_ELECTIVE',
    `default_capacity` INT UNSIGNED NULL COMMENT '默认教学班容量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_code` (`course_code`),
    KEY `idx_course_name` (`course_name`),
    KEY `idx_course_type` (`course_type`),

    CONSTRAINT `chk_course_credit`
        CHECK (`credit` > 0),
    CONSTRAINT `chk_course_default_capacity`
        CHECK (`default_capacity` IS NULL OR `default_capacity` > 0),
    CONSTRAINT `chk_course_type`
        CHECK (`course_type` IN ('REQUIRED', 'MAJOR_ELECTIVE', 'PUBLIC_ELECTIVE')),
    CONSTRAINT `chk_course_status`
        CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='课程基础信息表';

-- ============================================================
-- 10. 教学班表
-- ============================================================

CREATE TABLE `teaching_class` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教学班ID',
    `class_code` VARCHAR(50) NOT NULL COMMENT '教学班编号',
    `class_name` VARCHAR(150) NOT NULL COMMENT '教学班名称',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `semester_id` BIGINT UNSIGNED NOT NULL COMMENT '学期ID',
    `teacher_id` BIGINT UNSIGNED NOT NULL COMMENT '任课教师ID',
    `capacity` INT UNSIGNED NOT NULL COMMENT '最大选课人数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        COMMENT 'DRAFT/AVAILABLE/CLOSED/CANCELLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teaching_class_code` (`class_code`),
    KEY `idx_tc_course` (`course_id`),
    KEY `idx_tc_semester` (`semester_id`),
    KEY `idx_tc_teacher` (`teacher_id`),
    KEY `idx_tc_status` (`status`),
    KEY `idx_tc_semester_status` (`semester_id`, `status`),

    CONSTRAINT `fk_tc_course`
        FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_tc_semester`
        FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_tc_teacher`
        FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_tc_capacity`
        CHECK (`capacity` > 0),

    CONSTRAINT `chk_tc_status`
        CHECK (`status` IN ('DRAFT', 'AVAILABLE', 'CLOSED', 'CANCELLED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教学班表';

-- ============================================================
-- 11. 教学班适用专业表
-- ============================================================

CREATE TABLE `teaching_class_major` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `teaching_class_id` BIGINT UNSIGNED NOT NULL COMMENT '教学班ID',
    `major_id` BIGINT UNSIGNED NOT NULL COMMENT '适用专业ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tcm_class_major` (`teaching_class_id`, `major_id`),
    KEY `idx_tcm_major` (`major_id`),

    CONSTRAINT `fk_tcm_class`
        FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT `fk_tcm_major`
        FOREIGN KEY (`major_id`) REFERENCES `major` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教学班适用专业表';

-- ============================================================
-- 12. 教学班适用年级表
-- ============================================================

CREATE TABLE `teaching_class_grade` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `teaching_class_id` BIGINT UNSIGNED NOT NULL COMMENT '教学班ID',
    `grade_id` BIGINT UNSIGNED NOT NULL COMMENT '适用年级ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tcg_class_grade` (`teaching_class_id`, `grade_id`),
    KEY `idx_tcg_grade` (`grade_id`),

    CONSTRAINT `fk_tcg_class`
        FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT `fk_tcg_grade`
        FOREIGN KEY (`grade_id`) REFERENCES `grade_cohort` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教学班适用年级表';

-- ============================================================
-- 13. 教学班排课表
-- ============================================================

CREATE TABLE `teaching_class_schedule` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排课ID',
    `teaching_class_id` BIGINT UNSIGNED NOT NULL COMMENT '教学班ID',
    `classroom_id` BIGINT UNSIGNED NOT NULL COMMENT '教室ID',
    `weekday` TINYINT UNSIGNED NOT NULL COMMENT '星期：1周一...7周日',
    `start_section` TINYINT UNSIGNED NOT NULL COMMENT '开始节次',
    `end_section` TINYINT UNSIGNED NOT NULL COMMENT '结束节次',
    `start_week` TINYINT UNSIGNED NOT NULL COMMENT '开始周',
    `end_week` TINYINT UNSIGNED NOT NULL COMMENT '结束周',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_schedule_class` (`teaching_class_id`),
    KEY `idx_schedule_room` (`classroom_id`),
    KEY `idx_schedule_weekday` (`weekday`),
    KEY `idx_schedule_room_day` (`classroom_id`, `weekday`),

    CONSTRAINT `fk_schedule_class`
        FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT `fk_schedule_room`
        FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_schedule_weekday`
        CHECK (`weekday` BETWEEN 1 AND 7),
    CONSTRAINT `chk_schedule_section`
        CHECK (
            `start_section` >= 1
            AND `end_section` >= `start_section`
            AND `end_section` <= 20
        ),
    CONSTRAINT `chk_schedule_week`
        CHECK (
            `start_week` >= 1
            AND `end_week` >= `start_week`
            AND `end_week` <= 60
        )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教学班排课表';

-- ============================================================
-- 14. 选课批次表
-- ============================================================

CREATE TABLE `selection_batch` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '选课批次ID',
    `batch_name` VARCHAR(100) NOT NULL COMMENT '批次名称',
    `semester_id` BIGINT UNSIGNED NOT NULL COMMENT '所属学期ID',
    `start_time` DATETIME NOT NULL COMMENT '选课开始时间',
    `end_time` DATETIME NOT NULL COMMENT '选课结束时间',
    `drop_deadline` DATETIME NULL COMMENT '退课截止时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
        COMMENT 'NOT_STARTED/IN_PROGRESS/ENDED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_batch_semester` (`semester_id`),
    KEY `idx_batch_status` (`status`),
    KEY `idx_batch_semester_status` (`semester_id`, `status`),

    CONSTRAINT `fk_batch_semester`
        FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_batch_time`
        CHECK (`start_time` < `end_time`),
    CONSTRAINT `chk_batch_drop_deadline`
        CHECK (`drop_deadline` IS NULL OR `drop_deadline` >= `start_time`),
    CONSTRAINT `chk_batch_status`
        CHECK (`status` IN ('NOT_STARTED', 'IN_PROGRESS', 'ENDED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='选课批次表';

-- ============================================================
-- 15. 选课批次-教学班关联表
-- ============================================================

CREATE TABLE `selection_batch_class` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `batch_id` BIGINT UNSIGNED NOT NULL COMMENT '选课批次ID',
    `teaching_class_id` BIGINT UNSIGNED NOT NULL COMMENT '教学班ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sbc_batch_class` (`batch_id`, `teaching_class_id`),
    KEY `idx_sbc_class` (`teaching_class_id`),

    CONSTRAINT `fk_sbc_batch`
        FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT `fk_sbc_class`
        FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='选课批次开放教学班关联表';

-- ============================================================
-- 16. 选课记录表
-- ============================================================

CREATE TABLE `course_selection` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '选课记录ID',
    `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `teaching_class_id` BIGINT UNSIGNED NOT NULL COMMENT '教学班ID',
    `batch_id` BIGINT UNSIGNED NOT NULL COMMENT '选课批次ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'SELECTED'
        COMMENT 'SELECTED/WITHDRAWN',
    `selected_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次选课时间',
    `withdrawn_at` DATETIME NULL COMMENT '最近一次退课时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_selection_student_class` (`student_id`, `teaching_class_id`),
    KEY `idx_selection_student_status` (`student_id`, `status`),
    KEY `idx_selection_class_status` (`teaching_class_id`, `status`),
    KEY `idx_selection_batch` (`batch_id`),

    CONSTRAINT `fk_selection_student`
        FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_selection_class`
        FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `fk_selection_batch`
        FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_selection_status`
        CHECK (`status` IN ('SELECTED', 'WITHDRAWN'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学生选课记录表';

-- ============================================================
-- 17. 成绩表
-- ============================================================

CREATE TABLE `score` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
    `course_selection_id` BIGINT UNSIGNED NOT NULL COMMENT '对应选课记录ID',
    `score_value` DECIMAL(5,2) NOT NULL COMMENT '百分制成绩',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNPUBLISHED'
        COMMENT 'UNPUBLISHED/PUBLISHED',
    `published_at` DATETIME NULL COMMENT '成绩发布时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_score_selection` (`course_selection_id`),
    KEY `idx_score_status` (`status`),

    CONSTRAINT `fk_score_selection`
        FOREIGN KEY (`course_selection_id`) REFERENCES `course_selection` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT `chk_score_value`
        CHECK (`score_value` BETWEEN 0 AND 100),

    CONSTRAINT `chk_score_status`
        CHECK (`status` IN ('UNPUBLISHED', 'PUBLISHED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学生成绩表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 业务实现说明
-- ============================================================
--
-- 1. 学生课程可见权限：
--    student.major_id / student.grade_id
--    与 teaching_class_major / teaching_class_grade 匹配。
--
-- 2. 学生选课时，后端必须再次进行：
--    选课批次、专业、年级、重复选课、时间冲突、
--    学分上限、课程容量等校验。
--
-- 3. 教师、教室、学生时间冲突属于跨行/跨表业务校验，
--    建议在 Spring Boot Service 层完成。
--
-- 4. student.major_id / grade_id 必须与 academic_class 的
--    major_id / grade_id 保持一致，建议在 Service 层校验。
--
-- 5. selection_batch 与其绑定的 teaching_class 必须属于同一学期，
--    建议在 Service 层校验。
--
-- 6. V1 不单独建立课表表。
--    学生课表由 course_selection + teaching_class_schedule 动态生成。
--
-- 7. V1 不在 teaching_class 中维护 selected_count。
--    当前人数通过 course_selection WHERE status='SELECTED' 实时统计。
--
-- 8. V1.2 高并发阶段再增加 Redis + Lua + 数据库事务 + MQ。
-- ============================================================
