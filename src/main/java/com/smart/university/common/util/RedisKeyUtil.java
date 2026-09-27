package com.smart.university.common.util;

import com.smart.university.common.constant.RedisCommonConstant;

/**
 * Redis Key 构建工具
 */
public final class RedisKeyUtil {

    private RedisKeyUtil() {
    }

    /**
     * 构建登录 Token Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String buildLoginTokenKey(Long userId) {
        return String.format(RedisCommonConstant.LOGIN_TOKEN_KEY, userId);
    }

    /**
     * 构建登录用户缓存 Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String buildLoginUserKey(Long userId) {
        return String.format(RedisCommonConstant.LOGIN_USER_KEY, userId);
    }

    /**
     * 构建教学班剩余容量 Key
     *
     * @param teachingClassId 教学班 ID
     * @return Redis Key
     */
    public static String buildTeachingClassCapacityKey(Long teachingClassId) {
        return String.format(RedisCommonConstant.TEACHING_CLASS_CAPACITY_KEY, teachingClassId);
    }

    /**
     * 构建教学班开放专业缓存 Key
     *
     * @param teachingClassId 教学班 ID
     * @return Redis Key
     */
    public static String buildTeachingClassMajorKey(Long teachingClassId) {
        return String.format(RedisCommonConstant.TEACHING_CLASS_MAJOR_KEY, teachingClassId);
    }

    /**
     * 构建教学班开放年级缓存 Key
     *
     * @param teachingClassId 教学班 ID
     * @return Redis Key
     */
    public static String buildTeachingClassGradeKey(Long teachingClassId) {
        return String.format(RedisCommonConstant.TEACHING_CLASS_GRADE_KEY, teachingClassId);
    }

    /**
     * 构建学生已选课程时间占用 Key
     *
     * @param studentId 学生 ID
     * @return Redis Key
     */
    public static String buildStudentScheduleKey(Long studentId) {
        return String.format(RedisCommonConstant.STUDENT_SCHEDULE_KEY, studentId);
    }

    /**
     * 构建选课防重锁 Key
     *
     * @param studentId        学生 ID
     * @param teachingClassId  教学班 ID
     * @return Redis Key
     */
    public static String buildSelectionLockKey(Long studentId, Long teachingClassId) {
        return String.format(RedisCommonConstant.SELECTION_LOCK_KEY, studentId, teachingClassId);
    }

    /**
     * 构建当前生效选课批次缓存 Key
     *
     * @param semesterId 学期 ID
     * @return Redis Key
     */
    public static String buildCurrentSelectionBatchKey(Long semesterId) {
        return String.format(RedisCommonConstant.CURRENT_SELECTION_BATCH_KEY, semesterId);
    }
}
