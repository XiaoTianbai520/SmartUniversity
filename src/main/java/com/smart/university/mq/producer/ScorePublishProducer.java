package com.smart.university.mq.producer;

import com.smart.university.common.constant.RocketMQConstant;
import com.smart.university.mq.message.ScorePublishMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 成绩发布消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart-university.mq", name = "enabled", havingValue = "true")
public class ScorePublishProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送成绩发布消息
     *
     * @param message 成绩发布消息
     * @return 发送结果
     */
    public SendResult sendScorePublishMessage(ScorePublishMessage message) {
        String keys = String.valueOf(message.getTeachingClassId());
        String destination = RocketMQConstant.TOPIC + ":" + RocketMQConstant.SCORE_PUBLISH_TAG;
        Message<ScorePublishMessage> payload = MessageBuilder.withPayload(message)
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .build();
        long startTime = System.currentTimeMillis();
        SendResult result = null;
        try {
            result = rocketMQTemplate.syncSend(destination, payload, RocketMQConstant.SEND_TIMEOUT_MILLIS);
            return result;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            log.info("Send result: {}, Keys: {}, Cost time: {} ms, Payload: {}, Message: {}",
                    result == null ? "UNKNOWN" : result.getSendStatus(), keys, costTime, message, message);
        }
    }
}
