-- =====================================================================
-- V2.0 数据修复迁移脚本
-- 缺陷：M-03 data_audit_issue 唯一键缺少 biz_type，教室 ID 与教师 ID 撞号导致漏报
-- 说明：不把 stat_date 纳入唯一键，保持「一个问题一条」语义，避免按天膨胀
-- 执行：mysql -h<host> -P<port> -uroot -p <密码> -D edu_course_selection < docs/V2.0_数据修复迁移.sql
-- =====================================================================

ALTER TABLE `data_audit_issue`
  DROP INDEX `uk_audit_issue`,
  ADD UNIQUE KEY `uk_audit_issue` (`rule_code`, `biz_type`, `biz_id`);
