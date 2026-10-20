-- A1 脏数据残留全表扫描（只读）
USE edu_course_selection;
SELECT '=== A1.1 纯数字 5 位以上 ^[0-9]{5,}$ ===' AS section;
SELECT 'course.course_code' AS col, COUNT(*) AS hits FROM course WHERE course_code REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'course.course_name', COUNT(*) FROM course WHERE course_name REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'teaching_class.class_code', COUNT(*) FROM teaching_class WHERE class_code REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'teaching_class.class_name', COUNT(*) FROM teaching_class WHERE class_name REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'semester.semester_code', COUNT(*) FROM semester WHERE semester_code REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'semester.academic_year', COUNT(*) FROM semester WHERE academic_year REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'selection_batch.batch_name', COUNT(*) FROM selection_batch WHERE batch_name REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'student.student_no', COUNT(*) FROM student WHERE student_no REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'student.student_name', COUNT(*) FROM student WHERE student_name REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'teacher.teacher_no', COUNT(*) FROM teacher WHERE teacher_no REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'teacher.teacher_name', COUNT(*) FROM teacher WHERE teacher_name REGEXP '^[0-9]{5,}$'
UNION ALL SELECT 'sys_user.username', COUNT(*) FROM sys_user WHERE username REGEXP '^[0-9]{5,}$';

SELECT '=== A1.2 111/222/333/666 子串 ===' AS section;
SELECT 'course.course_code' AS col, COUNT(*) AS hits FROM course WHERE course_code REGEXP '(111|222|333|666)'
UNION ALL SELECT 'course.course_name', COUNT(*) FROM course WHERE course_name REGEXP '(111|222|333|666)'
UNION ALL SELECT 'teaching_class.class_code', COUNT(*) FROM teaching_class WHERE class_code REGEXP '(111|222|333|666)'
UNION ALL SELECT 'teaching_class.class_name', COUNT(*) FROM teaching_class WHERE class_name REGEXP '(111|222|333|666)'
UNION ALL SELECT 'semester.semester_code', COUNT(*) FROM semester WHERE semester_code REGEXP '(111|222|333|666)'
UNION ALL SELECT 'semester.academic_year', COUNT(*) FROM semester WHERE academic_year REGEXP '(111|222|333|666)'
UNION ALL SELECT 'selection_batch.batch_name', COUNT(*) FROM selection_batch WHERE batch_name REGEXP '(111|222|333|666)'
UNION ALL SELECT 'student.student_no', COUNT(*) FROM student WHERE student_no REGEXP '(111|222|333|666)'
UNION ALL SELECT 'student.student_name', COUNT(*) FROM student WHERE student_name REGEXP '(111|222|333|666)'
UNION ALL SELECT 'teacher.teacher_no', COUNT(*) FROM teacher WHERE teacher_no REGEXP '(111|222|333|666)'
UNION ALL SELECT 'teacher.teacher_name', COUNT(*) FROM teacher WHERE teacher_name REGEXP '(111|222|333|666)'
UNION ALL SELECT 'sys_user.username', COUNT(*) FROM sys_user WHERE username REGEXP '(111|222|333|666)';

SELECT '=== A1.3 test/测试/张三/李四/王五/张老师 ===' AS section;
SELECT 'course.course_name' AS col, COUNT(*) AS hits FROM course WHERE course_name REGEXP '(test|Test|TEST|测试|张三|李四|王五|张老师)'
UNION ALL SELECT 'course.course_code', COUNT(*) FROM course WHERE course_code REGEXP '(test|Test|TEST)'
UNION ALL SELECT 'teaching_class.class_name', COUNT(*) FROM teaching_class WHERE class_name REGEXP '(test|Test|TEST|测试|张三|李四|王五|张老师)'
UNION ALL SELECT 'teaching_class.class_code', COUNT(*) FROM teaching_class WHERE class_code REGEXP '(test|Test|TEST)'
UNION ALL SELECT 'student.student_name', COUNT(*) FROM student WHERE student_name REGEXP '(test|Test|TEST|测试|张三|李四|王五|张老师)'
UNION ALL SELECT 'student.student_no', COUNT(*) FROM student WHERE student_no REGEXP '(test|Test|TEST)'
UNION ALL SELECT 'teacher.teacher_name', COUNT(*) FROM teacher WHERE teacher_name REGEXP '(test|Test|TEST|测试|张三|李四|王五|张老师)'
UNION ALL SELECT 'teacher.teacher_no', COUNT(*) FROM teacher WHERE teacher_no REGEXP '(test|Test|TEST)'
UNION ALL SELECT 'sys_user.username', COUNT(*) FROM sys_user WHERE username REGEXP '(test|Test|TEST|ceshi)'
UNION ALL SELECT 'semester.semester_code', COUNT(*) FROM semester WHERE semester_code REGEXP '(test|Test)'
UNION ALL SELECT 'selection_batch.batch_name', COUNT(*) FROM selection_batch WHERE batch_name REGEXP '(test|Test|TEST|测试|1111)';

SELECT '=== A1.4 具体被点名删除的记录是否仍存在 ===' AS section;
SELECT 'semester id=8/code=111111' AS item, COUNT(*) AS hits FROM semester WHERE id=8 OR semester_code='111111'
UNION ALL SELECT 'course code 111/22222/33333/666666', COUNT(*) FROM course WHERE course_code IN ('111','22222','33333','666666')
UNION ALL SELECT 'teaching_class 111111/22222/33333/666666/2027Java/c011', COUNT(*) FROM teaching_class WHERE class_code IN ('111111','22222','33333','666666','2027Java','c011')
UNION ALL SELECT 'selection_batch 1111 / 2027-2028-1', COUNT(*) FROM selection_batch WHERE batch_name IN ('1111','2027-2028-1')
UNION ALL SELECT 'student no=name=11111', COUNT(*) FROM student WHERE student_no='11111' AND student_name='11111';

SELECT '=== A1.5 全表重复/占位姓名与编号抽样 ===' AS section;
SELECT 'course 近重复 name' AS item, COUNT(*) AS hits FROM (SELECT course_name FROM course GROUP BY course_name HAVING COUNT(*)>1) t
UNION ALL SELECT 'student 同名 >=20 人', COUNT(*) FROM (SELECT student_name FROM student GROUP BY student_name HAVING COUNT(*)>=20) t2
UNION ALL SELECT 'teacher 同名 >=3 人', COUNT(*) FROM (SELECT teacher_name FROM teacher GROUP BY teacher_name HAVING COUNT(*)>=3) t3;
