package com.smart.university.mq.consumer;

import com.smart.university.common.constant.RocketMQConstant;
import com.smart.university.mq.message.ScorePublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 成绩发布消息消费者，负责成绩发布后的下游通知处理，消费端保证幂等
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "smart-university.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = RocketMQConstant.TOPIC,
        consumerGroup = RocketMQConstant.SCORE_PUBLISH_CONSUMER_GROUP,
        selectorExpression = RocketMQConstant.SCORE_PUBLISH_TAG
)
public class ScorePublishConsumer implements RocketMQListener<ScorePublishMessage> {

    @Override
    public void onMessage(ScorePublishMessage message) {
        long startTime = System.currentTimeMillis();
        String keys = String.valueOf(message.getTeachingClassId());
        boolean executeResult;
        try {
            // V1 阶段仅记录日志，V1.1 可在此扩展站内信、消息推送等通知能力
            executeResult = true;
        } catch (Exception ex) {
            executeResult = false;
            log.error("成绩发布消息消费异常，Keys：{}", keys, ex);
        }
        long executeTime = System.currentTimeMillis() - startTime;
        log.info("Execute result: {}, Keys: {}, Dispatch time: {} ms, Execute time: {} ms, Message: {}",
                executeResult, keys, 0, executeTime, message);
    }
}
