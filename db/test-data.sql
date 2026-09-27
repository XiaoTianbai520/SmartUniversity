-- ============================================================
-- 高校智慧教务选课平台 V1 测试数据
-- 密码统一为 123456，管理员为 admin123，均使用 BCrypt 加密
-- ============================================================

USE `edu_course_selection`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清理旧数据（按外键依赖逆序）
DELETE FROM `score`;
DELETE FROM `course_selection`;
DELETE FROM `selection_batch_class`;
DELETE FROM `selection_batch`;
DELETE FROM `teaching_class_schedule`;
DELETE FROM `teaching_class_grade`;
DELETE FROM `teaching_class_major`;
DELETE FROM `teaching_class`;
DELETE FROM `course`;
DELETE FROM `semester`;
DELETE FROM `classroom`;
DELETE FROM `teacher`;
DELETE FROM `student`;
DELETE FROM `academic_class`;
DELETE FROM `grade_cohort`;
DELETE FROM `major`;
DELETE FROM `sys_user`;

ALTER TABLE `score` AUTO_INCREMENT = 1;
ALTER TABLE `course_selection` AUTO_INCREMENT = 1;
ALTER TABLE `selection_batch_class` AUTO_INCREMENT = 1;
ALTER TABLE `selection_batch` AUTO_INCREMENT = 1;
ALTER TABLE `teaching_class_schedule` AUTO_INCREMENT = 1;
ALTER TABLE `teaching_class_grade` AUTO_INCREMENT = 1;
ALTER TABLE `teaching_class_major` AUTO_INCREMENT = 1;
ALTER TABLE `teaching_class` AUTO_INCREMENT = 1;
ALTER TABLE `course` AUTO_INCREMENT = 1;
ALTER TABLE `semester` AUTO_INCREMENT = 1;
ALTER TABLE `classroom` AUTO_INCREMENT = 1;
ALTER TABLE `teacher` AUTO_INCREMENT = 1;
ALTER TABLE `student` AUTO_INCREMENT = 1;
ALTER TABLE `academic_class` AUTO_INCREMENT = 1;
ALTER TABLE `grade_cohort` AUTO_INCREMENT = 1;
ALTER TABLE `major` AUTO_INCREMENT = 1;
ALTER TABLE `sys_user` AUTO_INCREMENT = 1;

-- ============================================================
-- 1. 系统用户（1 管理员 + 2 教师 + 5 学生）
-- ============================================================

INSERT INTO `sys_user` (`id`, `username`, `password_hash`, `role`, `status`) VALUES
    (1, 'admin', '$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK', 'ADMIN', 1),
    (2, 'T10001', '$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK', 'TEACHER', 1),
    (3, 'T10002', '$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK', 'TEACHER', 1),
    (4, '20260001', '$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK', 'STUDENT', 1),
    (5, '20260002', '$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK', 'STUDENT', 1),
    (6, '20260003', '$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK', 'STUDENT', 1),
    (7, '20260004', '$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK', 'STUDENT', 1),
    (8, '20260005', '$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK', 'STUDENT', 1);

-- ============================================================
-- 2. 基础教务数据
-- ============================================================

INSERT INTO `major` (`id`, `major_code`, `major_name`, `status`) VALUES
    (1, 'SE', '软件工程', 1),
    (2, 'CS', '计算机科学与技术', 1);

INSERT INTO `grade_cohort` (`id`, `grade_name`, `entry_year`, `status`) VALUES
    (1, '2026级', 2026, 1);

INSERT INTO `academic_class` (`id`, `class_code`, `class_name`, `major_id`, `grade_id`, `status`) VALUES
    (1, 'SE202601', '软件工程2026级1班', 1, 1, 1),
    (2, 'CS202601', '计算机科学与技术2026级1班', 2, 1, 1);

INSERT INTO `classroom` (`id`, `building_name`, `room_no`, `capacity`, `status`) VALUES
    (1, '第一教学楼', 'A101', 80, 1),
    (2, '第一教学楼', 'A102', 60, 1),
    (3, '第二教学楼', 'B201', 60, 1);

INSERT INTO `semester` (`id`, `semester_code`, `academic_year`, `term_no`, `start_date`, `end_date`,
                        `max_selection_credit`, `status`) VALUES
    (1, '2026-2027-1', '2026-2027', 1, '2026-09-01', '2027-01-20', 30.0, 'ACTIVE');

-- ============================================================
-- 3. 教师与学生
-- ============================================================

INSERT INTO `teacher` (`id`, `user_id`, `teacher_no`, `teacher_name`, `title`, `status`) VALUES
    (1, 2, 'T10001', '张老师', '副教授', 1),
    (2, 3, 'T10002', '李老师', '讲师', 1);

INSERT INTO `student` (`id`, `user_id`, `student_no`, `student_name`, `major_id`, `grade_id`, `class_id`, `status`) VALUES
    (1, 4, '20260001', '张三', 1, 1, 1, 1),
    (2, 5, '20260002', '李四', 1, 1, 1, 1),
    (3, 6, '20260003', '王五', 1, 1, 1, 1),
    (4, 7, '20260004', '赵敏', 2, 1, 2, 1),
    (5, 8, '20260005', '孙悦', 2, 1, 2, 1);

-- ============================================================
-- 4. 课程
-- ============================================================

INSERT INTO `course` (`id`, `course_code`, `course_name`, `description`, `credit`, `course_type`,
                      `default_capacity`, `status`) VALUES
    (1, 'SE202', 'Java程序设计', 'Java基础与面向对象程序设计', 3.0, 'REQUIRED', 60, 1),
    (2, 'SE203', '数据结构', '线性表、树、图与常用算法', 4.0, 'REQUIRED', 50, 1),
    (3, 'CS101', '高等数学', '微积分与线性代数基础', 5.0, 'REQUIRED', 80, 1),
    (4, 'CS102', '机器学习导论', '机器学习基本概念与典型模型', 3.0, 'MAJOR_ELECTIVE', 40, 1),
    (5, 'GE001', '大学英语', '英语听说读写综合训练', 2.0, 'PUBLIC_ELECTIVE', 100, 1);

-- ============================================================
-- 5. 教学班与开放范围
-- ============================================================

INSERT INTO `teaching_class` (`id`, `class_code`, `class_name`, `course_id`, `semester_id`, `teacher_id`,
                              `capacity`, `status`) VALUES
    (1, '2026-JAVA-01', 'Java程序设计01班', 1, 1, 1, 60, 'AVAILABLE'),
    (2, '2026-DS-01', '数据结构01班', 2, 1, 2, 50, 'AVAILABLE'),
    (3, '2026-MATH-01', '高等数学01班', 3, 1, 1, 80, 'AVAILABLE'),
    (4, '2026-ML-01', '机器学习导论01班', 4, 1, 2, 40, 'AVAILABLE'),
    (5, '2026-ENG-01', '大学英语01班', 5, 1, 1, 100, 'AVAILABLE');

-- 机器学习导论仅对软件工程开放，用于验证专业过滤
INSERT INTO `teaching_class_major` (`teaching_class_id`, `major_id`) VALUES
    (1, 1), (1, 2),
    (2, 1),
    (3, 1), (3, 2),
    (4, 1),
    (5, 1), (5, 2);

INSERT INTO `teaching_class_grade` (`teaching_class_id`, `grade_id`) VALUES
    (1, 1), (2, 1), (3, 1), (4, 1), (5, 1);

-- ============================================================
-- 6. 排课（教师与教室均无时间冲突）
-- ============================================================

INSERT INTO `teaching_class_schedule` (`id`, `teaching_class_id`, `classroom_id`, `weekday`, `start_section`,
                                       `end_section`, `start_week`, `end_week`) VALUES
    (1, 1, 1, 1, 1, 2, 1, 16),
    (2, 1, 1, 3, 3, 4, 1, 16),
    (3, 2, 2, 2, 1, 2, 1, 16),
    (4, 2, 2, 4, 3, 4, 1, 16),
    (5, 3, 3, 1, 3, 4, 1, 16),
    (6, 3, 3, 3, 1, 2, 1, 16),
    (7, 4, 1, 2, 5, 6, 1, 16),
    (8, 5, 2, 5, 1, 2, 1, 16);

-- ============================================================
-- 7. 选课批次与开放教学班
-- ============================================================

INSERT INTO `selection_batch` (`id`, `batch_name`, `semester_id`, `start_time`, `end_time`, `drop_deadline`,
                               `status`) VALUES
    (1, '2026-2027第一学期第一轮选课', 1,
     '2026-09-20 08:00:00', '2026-10-10 23:59:59', '2026-10-15 23:59:59', 'IN_PROGRESS');

INSERT INTO `selection_batch_class` (`batch_id`, `teaching_class_id`) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5);

-- ============================================================
-- 8. 选课记录与成绩
-- ============================================================

INSERT INTO `course_selection` (`id`, `student_id`, `teaching_class_id`, `batch_id`, `status`,
                                `selected_at`, `withdrawn_at`) VALUES
    (1, 1, 1, 1, 'SELECTED', '2026-09-22 10:00:00', NULL),
    (2, 2, 3, 1, 'SELECTED', '2026-09-22 10:05:00', NULL),
    (3, 3, 1, 1, 'SELECTED', '2026-09-22 10:10:00', NULL),
    (4, 4, 2, 1, 'WITHDRAWN', '2026-09-22 10:15:00', '2026-09-23 09:00:00');

INSERT INTO `score` (`id`, `course_selection_id`, `score_value`, `status`, `published_at`) VALUES
    (1, 1, 88.5, 'PUBLISHED', '2026-09-26 10:00:00'),
    (2, 2, 92.0, 'UNPUBLISHED', NULL);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 数据核对
-- ============================================================

SELECT 'sys_user' AS `table`, COUNT(*) AS `rows` FROM `sys_user`
UNION ALL SELECT 'major', COUNT(*) FROM `major`
UNION ALL SELECT 'grade_cohort', COUNT(*) FROM `grade_cohort`
UNION ALL SELECT 'academic_class', COUNT(*) FROM `academic_class`
UNION ALL SELECT 'classroom', COUNT(*) FROM `classroom`
UNION ALL SELECT 'semester', COUNT(*) FROM `semester`
UNION ALL SELECT 'teacher', COUNT(*) FROM `teacher`
UNION ALL SELECT 'student', COUNT(*) FROM `student`
UNION ALL SELECT 'course', COUNT(*) FROM `course`
UNION ALL SELECT 'teaching_class', COUNT(*) FROM `teaching_class`
UNION ALL SELECT 'teaching_class_major', COUNT(*) FROM `teaching_class_major`
UNION ALL SELECT 'teaching_class_grade', COUNT(*) FROM `teaching_class_grade`
UNION ALL SELECT 'teaching_class_schedule', COUNT(*) FROM `teaching_class_schedule`
UNION ALL SELECT 'selection_batch', COUNT(*) FROM `selection_batch`
UNION ALL SELECT 'selection_batch_class', COUNT(*) FROM `selection_batch_class`
UNION ALL SELECT 'course_selection', COUNT(*) FROM `course_selection`
UNION ALL SELECT 'score', COUNT(*) FROM `score`;
