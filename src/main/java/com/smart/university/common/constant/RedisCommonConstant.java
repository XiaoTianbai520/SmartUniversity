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
     * 教学班候补人数缓存，拼接教学班 ID
     */
    public static final String TEACHING_CLASS_WAITING_COUNT_KEY = KEY_PREFIX + "teaching-class:waiting-count:%s";

    /**
     * 教学班候补序号自增，拼接教学班 ID
     */
    public static final String TEACHING_CLASS_WAITLIST_NO_KEY = KEY_PREFIX + "teaching-class:waitlist-no:%s";

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

    /**
     * 教学班候补人数缓存过期时间（秒）
     */
    public static final long TEACHING_CLASS_WAITING_COUNT_TTL_SECONDS = 24 * 60 * 60L;

    /**
     * V1.2 新增：教学班已选人数计数，拼接教学班 ID
     */
    public static final String TEACHING_CLASS_SELECTED_COUNT_KEY = KEY_PREFIX + "teaching-class:selected-count:%s";

    /**
     * V1.2 新增：分布式任务锁，拼接任务编码
     */
    public static final String TASK_LOCK_KEY = KEY_PREFIX + "lock:task:%s";

    /**
     * V2.0 新增：教务看板缓存，拼学期 ID
     */
    public static final String DASHBOARD_OVERVIEW_KEY = KEY_PREFIX + "dashboard:overview:%s";

    /**
     * V1.2 新增：选课容量缓存降级标记
     */
    public static final String SELECTION_DEGRADED_KEY = KEY_PREFIX + "selection:degraded";

    /**
     * V2.0 新增：统计快照生成时间，拼接学期 ID
     */
    public static final String STAT_SNAPSHOT_TIME_KEY = KEY_PREFIX + "stat:snapshot-time:%s";

    /**
     * V1.2 新增：教学班已选人数计数过期时间（秒）
     */
    public static final long TEACHING_CLASS_SELECTED_COUNT_TTL_SECONDS = 24 * 60 * 60L;

    /**
     * V2.0 新增：教务看板缓存过期时间（秒）
     */
    public static final long DASHBOARD_OVERVIEW_TTL_SECONDS = 15 * 60L;

    /**
     * V1.2 新增：选课容量缓存降级标记过期时间（秒）
     */
    public static final long SELECTION_DEGRADED_TTL_SECONDS = 60L;

    /**
     * V2.0 新增：统计快照生成时间过期时间（秒）
     */
    public static final long STAT_SNAPSHOT_TIME_TTL_SECONDS = 24 * 60 * 60L;
}
