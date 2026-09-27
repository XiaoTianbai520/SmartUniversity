package com.smart.university.mq.consumer;

import com.smart.university.common.constant.RedisCommonConstant;
import com.smart.university.common.constant.RocketMQConstant;
import com.smart.university.common.util.RedisKeyUtil;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mq.message.CourseSelectionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 选课消息消费者，负责刷新教学班容量缓存与学生课表缓存，消费端保证幂等
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart-university.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = RocketMQConstant.TOPIC,
        consumerGroup = RocketMQConstant.COURSE_SELECTION_CONSUMER_GROUP,
        selectorExpression = RocketMQConstant.COURSE_SELECTION_TAG + " || " + RocketMQConstant.COURSE_WITHDRAW_TAG
)
public class CourseSelectionConsumer implements RocketMQListener<CourseSelectionMessage> {

    private final CourseSelectionMapper courseSelectionMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(CourseSelectionMessage message) {
        long startTime = System.currentTimeMillis();
        String keys = message.getStudentId() + "_" + message.getTeachingClassId();
        boolean executeResult;
        try {
            executeResult = refreshCache(message);
        } catch (Exception ex) {
            executeResult = false;
            log.error("选课消息消费异常，Keys：{}", keys, ex);
        }
        long executeTime = System.currentTimeMillis() - startTime;
        log.info("Execute result: {}, Keys: {}, Dispatch time: {} ms, Execute time: {} ms, Message: {}",
                executeResult, keys, 0, executeTime, message);
    }

    /**
     * 刷新容量与课表缓存，重复消费结果一致
     *
     * @param message 选课结果消息
     * @return 处理成功返回 true
     */
    private boolean refreshCache(CourseSelectionMessage message) {
        long selectedCount = courseSelectionMapper.countSelectedByTeachingClassId(message.getTeachingClassId());
        stringRedisTemplate.opsForValue()
                .set(RedisKeyUtil.buildTeachingClassCapacityKey(message.getTeachingClassId()),
                        String.valueOf(selectedCount),
                        java.time.Duration.ofSeconds(RedisCommonConstant.TEACHING_CLASS_CAPACITY_TTL_SECONDS));
        // 课表由选课记录动态生成，退掉缓存即可保证下次查询为最新结果
        stringRedisTemplate.delete(RedisKeyUtil.buildStudentScheduleKey(message.getStudentId()));
        return true;
    }
}
