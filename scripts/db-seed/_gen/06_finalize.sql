SET NAMES utf8mb4;
SET SESSION sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
-- 冗余计数与巡检样本（task#4）
UPDATE teaching_class SET capacity = 57 WHERE id = 817;
UPDATE teaching_class SET capacity = 49 WHERE id = 857;
UPDATE teaching_class SET capacity = 47 WHERE id = 974;
UPDATE teaching_class SET capacity = 12 WHERE id = 737;
DELETE FROM teaching_class_schedule WHERE teaching_class_id = 800;
DELETE FROM teaching_class_schedule WHERE teaching_class_id = 935;
UPDATE teaching_class_schedule SET classroom_id=1, weekday=6, start_section=1, end_section=2, start_week=1, end_week=16 WHERE id=(SELECT sid FROM (SELECT MIN(id) sid FROM teaching_class_schedule WHERE teaching_class_id=738) t);
UPDATE teaching_class_schedule SET classroom_id=1, weekday=6, start_section=1, end_section=2, start_week=1, end_week=16 WHERE id=(SELECT sid FROM (SELECT MIN(id) sid FROM teaching_class_schedule WHERE teaching_class_id=740) t);
