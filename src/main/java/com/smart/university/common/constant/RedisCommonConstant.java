package com.smart.university.common.constant;

/**
 * 公共 Redis 常量，统一维护 Key 前缀与过期时间
 */
public final class RedisCommonConstant {

    private RedisCommonConstant() {
    }

    /**
     * 全站 Redis Key 前缀
     */
    public static final String KEY_PREFIX = "edu:smart-university:";

    /**
     * 登录 Token，拼接用户 ID
     */
    public static final String LOGIN_TOKEN_KEY = KEY_PREFIX + "login:token:%s";

    /**
     * 登录用户上下文缓存，拼接用户 ID
     */
    public static final String LOGIN_USER_KEY = KEY_PREFIX + "login:user:%s";

    /**
     * 教学班剩余容量，拼接教学班 ID
     */
    public static final String TEACHING_CLASS_CAPACITY_KEY = KEY_PREFIX + "teaching-class:capacity:%s";

    /**
     * 教学班开放专业缓存，拼接教学班 ID
     */
    public static final String TEACHING_CLASS_MAJOR_KEY = KEY_PREFIX + "teaching-class:major:%s";

    /**
     * 教学班开放年级缓存，拼接教学班 ID
     */
    public static final String TEACHING_CLASS_GRADE_KEY = KEY_PREFIX + "teaching-class:grade:%s";

    /**
     * 学生已选课程时间占用缓存，拼接学生 ID
     */
    public static final String STUDENT_SCHEDULE_KEY = KEY_PREFIX + "student:schedule:%s";

    /**
     * 选课防重提交锁，拼接学生 ID 与教学班 ID
     */
    public static final String SELECTION_LOCK_KEY = KEY_PREFIX + "selection:lock:%s:%s";

    /**
     * 当前生效学期缓存
     */
    public static final String CURRENT_SEMESTER_KEY = KEY_PREFIX + "semester:current";

    /**
     * 当前生效选课批次缓存，拼接学期 ID
     */
    public static final String CURRENT_SELECTION_BATCH_KEY = KEY_PREFIX + "selection-batch:current:%s";

    /**
     * 登录 Token 过期时间（秒）
     */
    public static final long LOGIN_TOKEN_TTL_SECONDS = 8 * 60 * 60L;

    /**
     * 教学班容量缓存过期时间（秒）
     */
    public static final long TEACHING_CLASS_CAPACITY_TTL_SECONDS = 24 * 60 * 60L;

    /**
     * 选课防重锁过期时间（秒）
     */
    public static final long SELECTION_LOCK_TTL_SECONDS = 10L;
}
