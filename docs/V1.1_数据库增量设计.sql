-- ============================================================
-- 高校智慧教务选课平台 V1.1 增量脚本
-- 前置：已执行 V1.0 建表脚本 docs/高校智慧教务选课平台_V1_数据库建表.sql
-- MySQL 8.x
-- ============================================================

USE `edu_course_selection`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 选课记录表扩展候补能力
-- ============================================================
-- 说明：
-- 1) V1.1 不新建候补表，候补记录复用 course_selection，
--    通过 uk_selection_student_class(student_id, teaching_class_id)
--    保证同一学生在同一教学班只能存在一条记录，天然避免
--    「已正式选中同时又候补」的数据冲突。
-- 2) status 扩展 WAITING（候补中），原 CHECK 约束需重建。
-- 3) waitlist_no 为同一教学班内的候补顺序号，从 1 开始递增。

ALTER TABLE `course_selection`
    MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'SELECTED'
        COMMENT 'SELECTED/WITHDRAWN/WAITING',
    ADD COLUMN `waitlist_no` INT UNSIGNED NULL COMMENT '候补序号，同一教学班内递增' AFTER `status`,
    ADD COLUMN `promoted_at` DATETIME NULL COMMENT '候补递补为正式选课的时间' AFTER `selected_at`,
    ADD KEY `idx_selection_waitlist` (`teaching_class_id`, `status`, `waitlist_no`);

ALTER TABLE `course_selection` DROP CHECK `chk_selection_status`;

ALTER TABLE `course_selection`
    ADD CONSTRAINT `chk_selection_status`
        CHECK (`status` IN ('SELECTED', 'WITHDRAWN', 'WAITING'));

-- ============================================================
-- 2. 站内通知表
-- ============================================================
-- 说明：
-- 1) 采用「一人一条」的站内信模型：每条通知针对一个接收人，
--    群发场景（如选课开始通知）在插入时复制多份。
--    该模型让「未读数」「标记已读」「通知列表」都能单表完成，
--    无需额外的通知-用户关联表。
-- 2) uk_notice_dedup 用于幂等：事件监听器重复消费同一条 MQ /
--    ApplicationEvent 时不会产生重复通知。
-- 3) biz_id 为业务单据 ID（批次 ID / 教学班 ID / 选课记录 ID），
--    用于跳转与去重，业务类型由 notice_type 区分。

DROP TABLE IF EXISTS `notification`;

CREATE TABLE `notification` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `notice_type` VARCHAR(30) NOT NULL COMMENT '通知类型：SELECTION_START/SELECTION_END/WAITLIST_PROMOTED/COURSE_ADJUSTED/SCORE_PUBLISHED',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` VARCHAR(1000) NOT NULL COMMENT '通知内容',
    `receiver_user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收人 sys_user.id',
    `receiver_role` VARCHAR(20) NOT NULL COMMENT '接收角色快照：STUDENT/TEACHER/ADMIN',
    `biz_id` BIGINT UNSIGNED NULL COMMENT '关联业务ID：批次ID/教学班ID/选课记录ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
    `read_at` DATETIME NULL COMMENT '读取时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_dedup` (`notice_type`, `biz_id`, `receiver_user_id`),
    KEY `idx_notice_receiver_read` (`receiver_user_id`, `is_read`),
    KEY `idx_notice_receiver_created` (`receiver_user_id`, `created_at`),
    KEY `idx_notice_type` (`notice_type`),
    KEY `idx_notice_biz` (`notice_type`, `biz_id`),

    CONSTRAINT `fk_notice_receiver`
        FOREIGN KEY (`receiver_user_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT `chk_notice_type`
        CHECK (`notice_type` IN ('SELECTION_START', 'SELECTION_END', 'WAITLIST_PROMOTED',
                                 'COURSE_ADJUSTED', 'SCORE_PUBLISHED')),
    CONSTRAINT `chk_notice_role`
        CHECK (`receiver_role` IN ('STUDENT', 'TEACHER', 'ADMIN')),
    CONSTRAINT `chk_notice_is_read`
        CHECK (`is_read` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='站内通知表，一人一条';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- V1.1 设计说明
-- ============================================================
--
-- 1. 候补与正式选课共用 course_selection，通过 status 区分。
--    统计「教学班已选人数」时只统计 status = 'SELECTED'；
--    统计「候补人数」时只统计 status = 'WAITING'。
--
-- 2. 候补递补发生在「释放名额」之后：正式选课学生退课、
--    或教务上调教学班容量，都会尝试把候补队列第一位
--    递补为正式选课。递补必须重新执行 V1.0 的全套选课校验。
--
-- 3. 通知不建立「已读记录表」，直接通过 notification.is_read
--    表达。群发时通过批量插入复制多份，配合 uk_notice_dedup
--    实现幂等。
--
-- 4. 成绩统计不需要新表，基于 score + course_selection
--    在 Service 层聚合计算即可。
--
-- 5. V1.1 不引入异步任务表；选课开始 / 结束通知由定时任务扫描
--    selection_batch 触发，幂等由 uk_notice_dedup 保证。
-- ============================================================
