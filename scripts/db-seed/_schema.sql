
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academic_class` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `class_code` varchar(30) NOT NULL COMMENT '班级编号',
  `class_name` varchar(100) NOT NULL COMMENT '班级名称',
  `major_id` bigint unsigned NOT NULL COMMENT '所属专业ID',
  `grade_id` bigint unsigned NOT NULL COMMENT '所属年级ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_academic_class_code` (`class_code`),
  KEY `idx_academic_class_major` (`major_id`),
  KEY `idx_academic_class_grade` (`grade_id`),
  CONSTRAINT `fk_academic_class_grade` FOREIGN KEY (`grade_id`) REFERENCES `grade_cohort` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_academic_class_major` FOREIGN KEY (`major_id`) REFERENCES `major` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_academic_class_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=162 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行政班级表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classroom` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '教室ID',
  `building_name` varchar(100) NOT NULL COMMENT '教学楼名称',
  `room_no` varchar(30) NOT NULL COMMENT '教室编号',
  `capacity` int unsigned NOT NULL COMMENT '教室容量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1可用，0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_classroom_building_room` (`building_name`,`room_no`),
  KEY `idx_classroom_building` (`building_name`),
  KEY `idx_classroom_status` (`status`),
  CONSTRAINT `chk_classroom_capacity` CHECK ((`capacity` > 0)),
  CONSTRAINT `chk_classroom_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教室表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_code` varchar(30) NOT NULL COMMENT '课程编号',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称',
  `description` text COMMENT '课程简介',
  `credit` decimal(4,1) NOT NULL COMMENT '课程学分',
  `course_type` varchar(30) NOT NULL COMMENT 'REQUIRED/MAJOR_ELECTIVE/PUBLIC_ELECTIVE',
  `default_capacity` int unsigned DEFAULT NULL COMMENT '默认教学班容量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_course_name` (`course_name`),
  KEY `idx_course_type` (`course_type`),
  CONSTRAINT `chk_course_credit` CHECK ((`credit` > 0)),
  CONSTRAINT `chk_course_default_capacity` CHECK (((`default_capacity` is null) or (`default_capacity` > 0))),
  CONSTRAINT `chk_course_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_course_type` CHECK ((`course_type` in (_utf8mb4'REQUIRED',_utf8mb4'MAJOR_ELECTIVE',_utf8mb4'PUBLIC_ELECTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=169 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程基础信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_selection` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '选课记录ID',
  `student_id` bigint unsigned NOT NULL COMMENT '学生ID',
  `teaching_class_id` bigint unsigned NOT NULL COMMENT '教学班ID',
  `batch_id` bigint unsigned NOT NULL COMMENT '选课批次ID',
  `status` varchar(20) NOT NULL DEFAULT 'SELECTED' COMMENT 'SELECTED/WITHDRAWN/WAITING',
  `waitlist_no` int unsigned DEFAULT NULL COMMENT '候补序号，同一教学班内递增',
  `selected_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次选课时间',
  `promoted_at` datetime DEFAULT NULL COMMENT '候补递补为正式选课的时间',
  `withdrawn_at` datetime DEFAULT NULL COMMENT '最近一次退课时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_selection_student_class` (`student_id`,`teaching_class_id`),
  KEY `idx_selection_student_status` (`student_id`,`status`),
  KEY `idx_selection_class_status` (`teaching_class_id`,`status`),
  KEY `idx_selection_batch` (`batch_id`),
  KEY `idx_selection_waitlist` (`teaching_class_id`,`status`,`waitlist_no`),
  KEY `idx_selection_batch_status` (`batch_id`,`status`),
  CONSTRAINT `fk_selection_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_selection_class` FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_selection_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_selection_status` CHECK ((`status` in (_utf8mb4'SELECTED',_utf8mb4'WITHDRAWN',_utf8mb4'WAITING')))
) ENGINE=InnoDB AUTO_INCREMENT=36578 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生选课记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_audit_issue` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '问题ID',
  `rule_code` varchar(40) NOT NULL COMMENT '巡检规则编码，例如 CAPACITY_ZERO / SCHEDULE_CONFLICT',
  `rule_name` varchar(100) NOT NULL COMMENT '规则中文名',
  `biz_type` varchar(30) NOT NULL COMMENT '业务类型：TEACHING_CLASS/CLASSROOM/TEACHER/SCORE/SEMESTER',
  `biz_id` bigint unsigned DEFAULT NULL COMMENT '关联业务ID',
  `severity` varchar(10) NOT NULL DEFAULT 'LOW' COMMENT '严重级别：LOW/MEDIUM/HIGH',
  `description` varchar(500) NOT NULL COMMENT '问题描述',
  `semester_id` bigint unsigned DEFAULT NULL COMMENT '所属学期ID，可为空',
  `stat_date` date NOT NULL COMMENT '巡检日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_issue` (`rule_code`,`biz_id`),
  KEY `idx_audit_issue_semester` (`semester_id`),
  KEY `idx_audit_issue_severity` (`severity`),
  KEY `idx_audit_issue_rule` (`rule_code`),
  KEY `idx_audit_issue_date` (`stat_date`),
  CONSTRAINT `chk_audit_issue_severity` CHECK ((`severity` in (_utf8mb4'LOW',_utf8mb4'MEDIUM',_utf8mb4'HIGH')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异常数据巡检结果表，只读不改业务数据';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_cohort` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '年级ID',
  `grade_name` varchar(30) NOT NULL COMMENT '年级名称，例如2026级',
  `entry_year` smallint unsigned NOT NULL COMMENT '入学年份',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grade_name` (`grade_name`),
  UNIQUE KEY `uk_grade_entry_year` (`entry_year`),
  CONSTRAINT `chk_grade_entry_year` CHECK ((`entry_year` between 2000 and 2200)),
  CONSTRAINT `chk_grade_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年级表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `major` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '专业ID',
  `major_code` varchar(30) NOT NULL COMMENT '专业编号',
  `major_name` varchar(100) NOT NULL COMMENT '专业名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_code` (`major_code`),
  CONSTRAINT `chk_major_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专业表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `notice_type` varchar(30) NOT NULL COMMENT '通知类型：SELECTION_START/SELECTION_END/WAITLIST_PROMOTED/COURSE_ADJUSTED/SCORE_PUBLISHED',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` varchar(1000) NOT NULL COMMENT '通知内容',
  `receiver_user_id` bigint unsigned NOT NULL COMMENT '接收人 sys_user.id',
  `receiver_role` varchar(20) NOT NULL COMMENT '接收角色快照：STUDENT/TEACHER/ADMIN',
  `biz_id` bigint unsigned DEFAULT NULL COMMENT '关联业务ID：批次ID/教学班ID/选课记录ID',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读：0未读，1已读',
  `read_at` datetime DEFAULT NULL COMMENT '读取时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_dedup` (`notice_type`,`biz_id`,`receiver_user_id`),
  KEY `idx_notice_receiver_read` (`receiver_user_id`,`is_read`),
  KEY `idx_notice_receiver_created` (`receiver_user_id`,`created_at`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_notice_biz` (`notice_type`,`biz_id`),
  CONSTRAINT `fk_notice_receiver` FOREIGN KEY (`receiver_user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_notice_is_read` CHECK ((`is_read` in (0,1))),
  CONSTRAINT `chk_notice_role` CHECK ((`receiver_role` in (_utf8mb4'STUDENT',_utf8mb4'TEACHER',_utf8mb4'ADMIN'))),
  CONSTRAINT `chk_notice_type` CHECK ((`notice_type` in (_utf8mb4'SELECTION_START',_utf8mb4'SELECTION_END',_utf8mb4'WAITLIST_PROMOTED',_utf8mb4'COURSE_ADJUSTED',_utf8mb4'SCORE_PUBLISHED',_utf8mb4'CREDIT_WARNING',_utf8mb4'SCORE_ANOMALY')))
) ENGINE=InnoDB AUTO_INCREMENT=32932 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内通知表，一人一条';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_task_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `task_code` varchar(40) NOT NULL COMMENT '任务编码，见 TaskCodeEnum',
  `task_name` varchar(100) NOT NULL COMMENT '任务中文名',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT '触发方式：SCHEDULED/MANUAL',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态：RUNNING/SUCCESS/FAILED/SKIPPED',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint unsigned DEFAULT NULL COMMENT '耗时（毫秒）',
  `scan_count` int unsigned NOT NULL DEFAULT '0' COMMENT '扫描条数',
  `success_count` int unsigned NOT NULL DEFAULT '0' COMMENT '成功条数',
  `fail_count` int unsigned NOT NULL DEFAULT '0' COMMENT '失败条数',
  `error_summary` varchar(1000) DEFAULT NULL COMMENT '异常摘要',
  `instance_id` varchar(100) DEFAULT NULL COMMENT '执行实例标识（主机名:端口）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_log_code` (`task_code`),
  KEY `idx_task_log_code_start` (`task_code`,`start_time`),
  KEY `idx_task_log_status` (`status`),
  KEY `idx_task_log_created` (`created_at`),
  CONSTRAINT `chk_task_log_status` CHECK ((`status` in (_utf8mb4'RUNNING',_utf8mb4'SUCCESS',_utf8mb4'FAILED',_utf8mb4'SKIPPED'))),
  CONSTRAINT `chk_task_log_trigger_type` CHECK ((`trigger_type` in (_utf8mb4'SCHEDULED',_utf8mb4'MANUAL')))
) ENGINE=InnoDB AUTO_INCREMENT=4562 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务执行日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `score` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
  `course_selection_id` bigint unsigned NOT NULL COMMENT '对应选课记录ID',
  `score_value` decimal(5,2) NOT NULL COMMENT '百分制成绩',
  `status` varchar(20) NOT NULL DEFAULT 'UNPUBLISHED' COMMENT 'UNPUBLISHED/PUBLISHED',
  `published_at` datetime DEFAULT NULL COMMENT '成绩发布时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_score_selection` (`course_selection_id`),
  KEY `idx_score_status` (`status`),
  KEY `idx_score_status_value` (`status`,`score_value`),
  KEY `idx_score_published_at` (`status`,`published_at`),
  CONSTRAINT `fk_score_selection` FOREIGN KEY (`course_selection_id`) REFERENCES `course_selection` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_score_status` CHECK ((`status` in (_utf8mb4'UNPUBLISHED',_utf8mb4'PUBLISHED'))),
  CONSTRAINT `chk_score_value` CHECK ((`score_value` between 0 and 100))
) ENGINE=InnoDB AUTO_INCREMENT=25361 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生成绩表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `selection_batch` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '选课批次ID',
  `batch_name` varchar(100) NOT NULL COMMENT '批次名称',
  `semester_id` bigint unsigned NOT NULL COMMENT '所属学期ID',
  `start_time` datetime NOT NULL COMMENT '选课开始时间',
  `end_time` datetime NOT NULL COMMENT '选课结束时间',
  `drop_deadline` datetime DEFAULT NULL COMMENT '退课截止时间',
  `status` varchar(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED/IN_PROGRESS/ENDED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_batch_semester` (`semester_id`),
  KEY `idx_batch_status` (`status`),
  KEY `idx_batch_semester_status` (`semester_id`,`status`),
  KEY `idx_batch_status_time` (`status`,`start_time`,`end_time`),
  CONSTRAINT `fk_batch_semester` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_batch_drop_deadline` CHECK (((`drop_deadline` is null) or (`drop_deadline` >= `start_time`))),
  CONSTRAINT `chk_batch_status` CHECK ((`status` in (_utf8mb4'NOT_STARTED',_utf8mb4'IN_PROGRESS',_utf8mb4'ENDED'))),
  CONSTRAINT `chk_batch_time` CHECK ((`start_time` < `end_time`))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='选课批次表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `selection_batch_class` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` bigint unsigned NOT NULL COMMENT '选课批次ID',
  `teaching_class_id` bigint unsigned NOT NULL COMMENT '教学班ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sbc_batch_class` (`batch_id`,`teaching_class_id`),
  KEY `idx_sbc_class` (`teaching_class_id`),
  CONSTRAINT `fk_sbc_batch` FOREIGN KEY (`batch_id`) REFERENCES `selection_batch` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sbc_class` FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1116 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='选课批次开放教学班关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semester` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '学期ID',
  `semester_code` varchar(30) NOT NULL COMMENT '学期编号，例如2026-2027-1',
  `academic_year` varchar(20) NOT NULL COMMENT '学年，例如2026-2027',
  `term_no` tinyint unsigned NOT NULL COMMENT '学期序号',
  `start_date` date NOT NULL COMMENT '学期开始日期',
  `end_date` date NOT NULL COMMENT '学期结束日期',
  `max_selection_credit` decimal(5,1) NOT NULL DEFAULT '30.0' COMMENT '学生本学期最大可选学分',
  `status` varchar(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/ACTIVE/FINISHED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_semester_code` (`semester_code`),
  KEY `idx_semester_status` (`status`),
  CONSTRAINT `chk_semester_credit` CHECK ((`max_selection_credit` > 0)),
  CONSTRAINT `chk_semester_date` CHECK ((`start_date` < `end_date`)),
  CONSTRAINT `chk_semester_status` CHECK ((`status` in (_utf8mb4'PLANNED',_utf8mb4'ACTIVE',_utf8mb4'FINISHED'))),
  CONSTRAINT `chk_semester_term_no` CHECK ((`term_no` between 1 and 3))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学期表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semester_archive` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '归档ID',
  `semester_id` bigint unsigned NOT NULL COMMENT '学期ID',
  `archive_type` varchar(30) NOT NULL COMMENT '归档类型：SELECTION/SCORE/OVERVIEW',
  `archive_content` json DEFAULT NULL COMMENT '归档内容（JSON）',
  `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_semester_archive` (`semester_id`,`archive_type`),
  KEY `idx_semester_archive_semester` (`semester_id`),
  CONSTRAINT `fk_semester_archive_semester` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_semester_archive_type` CHECK ((`archive_type` in (_utf8mb4'SELECTION',_utf8mb4'SCORE',_utf8mb4'OVERVIEW')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学期末数据归档表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stat_classroom_usage` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `semester_id` bigint unsigned NOT NULL COMMENT '学期ID',
  `classroom_id` bigint unsigned NOT NULL COMMENT '教室ID',
  `building_name` varchar(100) NOT NULL COMMENT '教学楼名称快照',
  `room_no` varchar(30) NOT NULL COMMENT '教室编号快照',
  `capacity` int unsigned NOT NULL DEFAULT '0' COMMENT '教室容量快照',
  `usage_count` int unsigned NOT NULL DEFAULT '0' COMMENT '使用次数（排课条数）',
  `usage_hours` int unsigned NOT NULL DEFAULT '0' COMMENT '使用课时（节次×周数之和）',
  `total_hours` int unsigned NOT NULL DEFAULT '0' COMMENT '可排课时总数（分母）',
  `usage_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '利用率百分比，0~100，保留 2 位小数',
  `stat_date` date NOT NULL COMMENT '快照日期',
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_classroom` (`semester_id`,`classroom_id`,`stat_date`),
  KEY `idx_stat_classroom_semester` (`semester_id`),
  KEY `idx_stat_classroom_building` (`building_name`),
  KEY `idx_stat_classroom_rate` (`usage_rate`),
  KEY `fk_stat_classroom_classroom` (`classroom_id`),
  CONSTRAINT `fk_stat_classroom_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_stat_classroom_semester` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=85 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教室利用率统计快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stat_course_selection` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `semester_id` bigint unsigned NOT NULL COMMENT '学期ID',
  `course_id` bigint unsigned NOT NULL COMMENT '课程ID',
  `teaching_class_id` bigint unsigned NOT NULL COMMENT '教学班ID',
  `course_code` varchar(30) NOT NULL COMMENT '课程编号快照',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称快照',
  `class_code` varchar(50) NOT NULL COMMENT '教学班编号快照',
  `teacher_id` bigint unsigned DEFAULT NULL COMMENT '任课教师ID',
  `teacher_name` varchar(50) DEFAULT NULL COMMENT '任课教师姓名快照',
  `capacity` int unsigned NOT NULL DEFAULT '0' COMMENT '教学班容量',
  `selected_count` int unsigned NOT NULL DEFAULT '0' COMMENT '已选人数（DB 权威口径，不含候补）',
  `waiting_count` int unsigned NOT NULL DEFAULT '0' COMMENT '候补人数',
  `withdraw_count` int unsigned NOT NULL DEFAULT '0' COMMENT '退课人数（仅 WITHDRAWN）',
  `full_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否满班：1满班（selected_count >= capacity），0未满',
  `stat_date` date NOT NULL COMMENT '快照日期',
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_course` (`semester_id`,`teaching_class_id`,`stat_date`),
  KEY `idx_stat_course_semester` (`semester_id`),
  KEY `idx_stat_course_course` (`course_id`),
  KEY `idx_stat_course_selected` (`selected_count`),
  KEY `idx_stat_course_full` (`full_flag`),
  KEY `fk_stat_course_class` (`teaching_class_id`),
  CONSTRAINT `fk_stat_course_class` FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_stat_course_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_stat_course_semester` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程选课分析统计快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stat_credit_progress` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `semester_id` bigint unsigned NOT NULL COMMENT '学期ID',
  `student_id` bigint unsigned NOT NULL COMMENT '学生ID',
  `student_no` varchar(30) NOT NULL COMMENT '学号快照',
  `student_name` varchar(50) NOT NULL COMMENT '学生姓名快照',
  `major_id` bigint unsigned DEFAULT NULL COMMENT '所属专业ID快照',
  `grade_id` bigint unsigned DEFAULT NULL COMMENT '所属年级ID快照',
  `has_plan` tinyint NOT NULL DEFAULT '0' COMMENT '是否存在培养方案：1有，0无',
  `completed_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '已完成总学分（跨学期累计，已发布即计入，不判及格）',
  `passed_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '及格学分（跨学期累计，score_value>=60，附加展示字段，不参与完成度与风险计算）',
  `required_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '已完成的必修学分',
  `major_elective_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '已完成的专业选修学分',
  `public_elective_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '已完成的公共选修学分',
  `plan_total_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '培养方案毕业总学分要求',
  `plan_required_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '培养方案必修学分要求',
  `plan_major_elective_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '培养方案专业选修学分要求',
  `plan_public_elective_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '培养方案公共选修学分要求',
  `progress_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '总体完成度百分比，0~100',
  `risk_level` varchar(10) NOT NULL DEFAULT 'LOW' COMMENT '毕业风险等级：LOW/MEDIUM/HIGH',
  `stat_date` date NOT NULL COMMENT '快照日期',
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_credit` (`semester_id`,`student_id`,`stat_date`),
  KEY `idx_stat_credit_semester` (`semester_id`),
  KEY `idx_stat_credit_major_grade` (`major_id`,`grade_id`),
  KEY `idx_stat_credit_risk` (`risk_level`),
  KEY `idx_stat_credit_rate` (`progress_rate`),
  KEY `fk_stat_credit_student` (`student_id`),
  CONSTRAINT `fk_stat_credit_semester` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_stat_credit_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_stat_credit_has_plan` CHECK ((`has_plan` in (0,1))),
  CONSTRAINT `chk_stat_credit_rate` CHECK ((`progress_rate` between 0 and 100)),
  CONSTRAINT `chk_stat_credit_risk_level` CHECK ((`risk_level` in (_utf8mb4'LOW',_utf8mb4'MEDIUM',_utf8mb4'HIGH')))
) ENGINE=InnoDB AUTO_INCREMENT=4047 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生学分进度统计快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stat_semester_overview` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `semester_id` bigint unsigned NOT NULL COMMENT '学期ID',
  `semester_code` varchar(30) NOT NULL COMMENT '学期编号快照',
  `student_count` int unsigned NOT NULL DEFAULT '0' COMMENT '在册学生数（COUNT(student WHERE status=1)）',
  `course_count` int unsigned NOT NULL DEFAULT '0' COMMENT '本学期开设课程数（DISTINCT course_id）',
  `teaching_class_count` int unsigned NOT NULL DEFAULT '0' COMMENT '本学期教学班数',
  `teacher_count` int unsigned NOT NULL DEFAULT '0' COMMENT '本学期任课教师数（DISTINCT teacher_id）',
  `selection_count` int unsigned NOT NULL DEFAULT '0' COMMENT '当前选课人次（status=SELECTED）',
  `waitlist_count` int unsigned NOT NULL DEFAULT '0' COMMENT '当前候补人次（status=WAITING）',
  `withdraw_count` int unsigned NOT NULL DEFAULT '0' COMMENT '本学期退课人次（status=WITHDRAWN）',
  `full_class_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '课程满班率百分比，0~100',
  `classroom_usage_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '教室利用率百分比，0~100',
  `avg_teacher_workload` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '教师平均工作量（课时）',
  `avg_credit_progress_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '平均学分完成度百分比',
  `unpublished_class_count` int unsigned NOT NULL DEFAULT '0' COMMENT '待发布成绩教学班数',
  `batch_status` varchar(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '当前选课批次状态：NOT_STARTED/IN_PROGRESS/ENDED',
  `stat_date` date NOT NULL COMMENT '快照日期',
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间，作为接口 snapshotTime',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_overview` (`semester_id`,`stat_date`),
  KEY `idx_stat_overview_semester` (`semester_id`),
  KEY `idx_stat_overview_generated` (`generated_at`),
  CONSTRAINT `fk_stat_overview_semester` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_stat_overview_batch_status` CHECK ((`batch_status` in (_utf8mb4'NOT_STARTED',_utf8mb4'IN_PROGRESS',_utf8mb4'ENDED')))
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教务数据看板指标快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stat_teacher_workload` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `semester_id` bigint unsigned NOT NULL COMMENT '学期ID',
  `teacher_id` bigint unsigned NOT NULL COMMENT '教师ID',
  `teacher_no` varchar(30) NOT NULL COMMENT '教师编号快照',
  `teacher_name` varchar(50) NOT NULL COMMENT '教师姓名快照',
  `class_count` int unsigned NOT NULL DEFAULT '0' COMMENT '教学班数',
  `total_hours` int unsigned NOT NULL DEFAULT '0' COMMENT '总课时（节次×周数之和）',
  `student_count` int unsigned NOT NULL DEFAULT '0' COMMENT '学生总人数（去重后）',
  `stat_date` date NOT NULL COMMENT '快照日期',
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_teacher` (`semester_id`,`teacher_id`,`stat_date`),
  KEY `idx_stat_teacher_semester` (`semester_id`),
  KEY `idx_stat_teacher_teacher` (`teacher_id`),
  CONSTRAINT `fk_stat_teacher_semester` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_stat_teacher_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师工作量统计快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `user_id` bigint unsigned NOT NULL COMMENT '对应系统用户ID',
  `student_no` varchar(30) NOT NULL COMMENT '学号',
  `student_name` varchar(50) NOT NULL COMMENT '学生姓名',
  `major_id` bigint unsigned NOT NULL COMMENT '所属专业ID',
  `grade_id` bigint unsigned NOT NULL COMMENT '所属年级ID',
  `class_id` bigint unsigned NOT NULL COMMENT '所属行政班级ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '学生状态：1正常，0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_user` (`user_id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_student_major` (`major_id`),
  KEY `idx_student_grade` (`grade_id`),
  KEY `idx_student_class` (`class_id`),
  KEY `idx_student_status` (`status`),
  CONSTRAINT `fk_student_class` FOREIGN KEY (`class_id`) REFERENCES `academic_class` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_student_grade` FOREIGN KEY (`grade_id`) REFERENCES `grade_cohort` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_student_major` FOREIGN KEY (`major_id`) REFERENCES `major` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_student_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=2002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希，不允许明文保存',
  `role` varchar(20) NOT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态：1启用，0禁用',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  CONSTRAINT `chk_sys_user_role` CHECK ((`role` in (_utf8mb4'STUDENT',_utf8mb4'TEACHER',_utf8mb4'ADMIN'))),
  CONSTRAINT `chk_sys_user_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=2184 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `user_id` bigint unsigned NOT NULL COMMENT '对应系统用户ID',
  `teacher_no` varchar(30) NOT NULL COMMENT '教师编号',
  `teacher_name` varchar(50) NOT NULL COMMENT '教师姓名',
  `title` varchar(50) DEFAULT NULL COMMENT '职称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '教师状态：1正常，0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_user` (`user_id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`),
  CONSTRAINT `fk_teacher_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_teacher_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=181 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_class` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '教学班ID',
  `class_code` varchar(50) NOT NULL COMMENT '教学班编号',
  `class_name` varchar(150) NOT NULL COMMENT '教学班名称',
  `course_id` bigint unsigned NOT NULL COMMENT '课程ID',
  `semester_id` bigint unsigned NOT NULL COMMENT '学期ID',
  `teacher_id` bigint unsigned NOT NULL COMMENT '任课教师ID',
  `capacity` int unsigned NOT NULL COMMENT '最大选课人数',
  `selected_count` int unsigned NOT NULL DEFAULT '0' COMMENT '已选人数冗余计数，仅供并发写入条件更新使用，统计口径以 course_selection 为准',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/AVAILABLE/CLOSED/CANCELLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teaching_class_code` (`class_code`),
  KEY `idx_tc_course` (`course_id`),
  KEY `idx_tc_semester` (`semester_id`),
  KEY `idx_tc_teacher` (`teacher_id`),
  KEY `idx_tc_status` (`status`),
  KEY `idx_tc_semester_status` (`semester_id`,`status`),
  KEY `idx_tc_semester_teacher` (`semester_id`,`teacher_id`),
  KEY `idx_tc_semester_status_teacher` (`semester_id`,`status`,`teacher_id`),
  CONSTRAINT `fk_tc_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tc_semester` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_tc_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_tc_capacity` CHECK ((`capacity` > 0)),
  CONSTRAINT `chk_tc_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'AVAILABLE',_utf8mb4'CLOSED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=1113 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学班表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_class_grade` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teaching_class_id` bigint unsigned NOT NULL COMMENT '教学班ID',
  `grade_id` bigint unsigned NOT NULL COMMENT '适用年级ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tcg_class_grade` (`teaching_class_id`,`grade_id`),
  KEY `idx_tcg_grade` (`grade_id`),
  CONSTRAINT `fk_tcg_class` FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_tcg_grade` FOREIGN KEY (`grade_id`) REFERENCES `grade_cohort` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5576 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学班适用年级表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_class_major` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teaching_class_id` bigint unsigned NOT NULL COMMENT '教学班ID',
  `major_id` bigint unsigned NOT NULL COMMENT '适用专业ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tcm_class_major` (`teaching_class_id`,`major_id`),
  KEY `idx_tcm_major` (`major_id`),
  CONSTRAINT `fk_tcm_class` FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_tcm_major` FOREIGN KEY (`major_id`) REFERENCES `major` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5012 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学班适用专业表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_class_schedule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '排课ID',
  `teaching_class_id` bigint unsigned NOT NULL COMMENT '教学班ID',
  `classroom_id` bigint unsigned NOT NULL COMMENT '教室ID',
  `weekday` tinyint unsigned NOT NULL COMMENT '星期：1周一...7周日',
  `start_section` tinyint unsigned NOT NULL COMMENT '开始节次',
  `end_section` tinyint unsigned NOT NULL COMMENT '结束节次',
  `start_week` tinyint unsigned NOT NULL COMMENT '开始周',
  `end_week` tinyint unsigned NOT NULL COMMENT '结束周',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_schedule_class` (`teaching_class_id`),
  KEY `idx_schedule_room` (`classroom_id`),
  KEY `idx_schedule_weekday` (`weekday`),
  KEY `idx_schedule_room_day` (`classroom_id`,`weekday`),
  KEY `idx_schedule_weekday_section` (`weekday`,`start_section`,`end_section`),
  CONSTRAINT `fk_schedule_class` FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_class` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_schedule_room` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_schedule_section` CHECK (((`start_section` >= 1) and (`end_section` >= `start_section`) and (`end_section` <= 20))),
  CONSTRAINT `chk_schedule_week` CHECK (((`start_week` >= 1) and (`end_week` >= `start_week`) and (`end_week` <= 60))),
  CONSTRAINT `chk_schedule_weekday` CHECK ((`weekday` between 1 and 7))
) ENGINE=InnoDB AUTO_INCREMENT=2217 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学班排课表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `training_plan` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '培养方案ID',
  `plan_name` varchar(100) NOT NULL COMMENT '培养方案名称，例如：软件工程2026级培养方案',
  `major_id` bigint unsigned NOT NULL COMMENT '所属专业ID',
  `grade_id` bigint unsigned NOT NULL COMMENT '所属年级ID（入学年级）',
  `total_credit` decimal(5,1) NOT NULL COMMENT '毕业总学分要求',
  `required_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '必修学分要求',
  `major_elective_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '专业选修学分要求',
  `public_elective_credit` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '公共选修学分要求',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_training_plan_major_grade` (`major_id`,`grade_id`),
  KEY `idx_training_plan_major` (`major_id`),
  KEY `idx_training_plan_grade` (`grade_id`),
  KEY `idx_training_plan_status` (`status`),
  CONSTRAINT `fk_training_plan_grade` FOREIGN KEY (`grade_id`) REFERENCES `grade_cohort` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_training_plan_major` FOREIGN KEY (`major_id`) REFERENCES `major` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_training_plan_credit` CHECK (((`total_credit` > 0) and (`required_credit` >= 0) and (`major_elective_credit` >= 0) and (`public_elective_credit` >= 0) and (((`required_credit` + `major_elective_credit`) + `public_elective_credit`) <= `total_credit`))),
  CONSTRAINT `chk_training_plan_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培养方案表，专业 + 入学年级的毕业学分要求';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

