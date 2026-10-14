package com.smart.university.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Jackson 时间格式配置
 * <p>
 * 出参维持 ISO-8601（形如 {@code 2026-09-15T08:00:00}，中间为大写 T），与接口文档约定一致；
 * 入参放宽为同时兼容 T 分隔与空格分隔（{@code 2026-09-15T08:00:00} / {@code 2026-09-15 08:00:00}），秒可省略。
 * <p>
 * 放宽入参的原因：Jackson 对 {@link LocalDateTime} 默认只接受 ISO-8601，
 * 空格格式会直接抛格式异常导致整个请求报 40001，而接口文档约定两种写法都应能正常反序列化。
 */
@Configuration
public class JacksonConfiguration {

    /**
     * 入参兼容格式：yyyy-MM-ddTHH:mm[:ss] 与 yyyy-MM-dd HH:mm[:ss]
     */
    private static final DateTimeFormatter LENIENT_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
            .toFormatter();

    /**
     * 放宽 LocalDateTime 入参解析，出参序列化格式不受影响，仍为 ISO-8601
     *
     * @return Jackson 定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer lenientLocalDateTimeCustomizer() {
        return builder -> builder.deserializerByType(LocalDateTime.class,
                new LocalDateTimeDeserializer(LENIENT_DATE_TIME_FORMATTER));
    }
}
