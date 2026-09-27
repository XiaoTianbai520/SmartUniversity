package com.smart.university.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成绩发布消息，用于异步通知学生与刷新统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScorePublishMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 本次发布的选课记录 ID 集合
     */
    private List<Long> selectionIds;

    /**
     * 发布数量
     */
    private Integer publishedCount;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;
}
