package com.smart.university.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.smart.university.common.constant.RocketMQConstant;
import com.smart.university.mq.message.CourseSelectionMessage;
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
 * 选课消息生产者，一个应用共用一个生产者组，不同业务通过 Tag 区分
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart-university.mq", name = "enabled", havingValue = "true")
public class CourseSelectionProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送选课结果消息
     *
     * @param message 选课结果消息
     * @return 发送结果
     */
    public SendResult sendCourseSelectionMessage(CourseSelectionMessage message) {
        String keys = buildKeys(message);
        String destination = RocketMQConstant.TOPIC + ":" + RocketMQConstant.COURSE_SELECTION_TAG;
        Message<CourseSelectionMessage> payload = MessageBuilder.withPayload(message)
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

    /**
     * 发送退课结果消息
     *
     * @param message 选课结果消息，操作类型为 WITHDRAW
     * @return 发送结果
     */
    public SendResult sendCourseWithdrawMessage(CourseSelectionMessage message) {
        String keys = buildKeys(message);
        String destination = RocketMQConstant.TOPIC + ":" + RocketMQConstant.COURSE_WITHDRAW_TAG;
        Message<CourseSelectionMessage> payload = MessageBuilder.withPayload(message)
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

    private String buildKeys(CourseSelectionMessage message) {
        if (message == null || message.getStudentId() == null || message.getTeachingClassId() == null) {
            return StrUtil.EMPTY;
        }
        return message.getStudentId() + "_" + message.getTeachingClassId();
    }
}
