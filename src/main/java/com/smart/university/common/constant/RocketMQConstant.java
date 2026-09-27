package com.smart.university.common.constant;

/**
 * RocketMQ 常量，Topic / Tag / 生产者组 / 消费者组统一在此定义
 *
 * <p>命名规范：业务线_项目名_topic、业务线_项目_业务_tag、业务线_项目_业务_pg、业务线_项目_业务_cg。</p>
 */
public final class RocketMQConstant {

    private RocketMQConstant() {
    }

    /**
     * 项目统一 Topic，一个系统一个 Topic，业务通过 Tag 区分
     */
    public static final String TOPIC = "edu_smart-university_topic";

    /**
     * 选课结果异步落库 Tag
     */
    public static final String COURSE_SELECTION_TAG = "edu_smart-university_course-selection_tag";

    /**
     * 退课结果异步处理 Tag
     */
    public static final String COURSE_WITHDRAW_TAG = "edu_smart-university_course-withdraw_tag";

    /**
     * 成绩发布通知 Tag
     */
    public static final String SCORE_PUBLISH_TAG = "edu_smart-university_score-publish_tag";

    /**
     * 选课消息生产者组
     */
    public static final String COURSE_SELECTION_PRODUCER_GROUP = "edu_smart-university_course-selection_pg";

    /**
     * 选课消息消费者组
     */
    public static final String COURSE_SELECTION_CONSUMER_GROUP = "edu_smart-university_course-selection_cg";

    /**
     * 成绩发布消息生产者组
     */
    public static final String SCORE_PUBLISH_PRODUCER_GROUP = "edu_smart-university_score-publish_pg";

    /**
     * 成绩发布消息消费者组
     */
    public static final String SCORE_PUBLISH_CONSUMER_GROUP = "edu_smart-university_score-publish_cg";

    /**
     * 消息发送超时时间（毫秒）
     */
    public static final long SEND_TIMEOUT_MILLIS = 2000L;
}
