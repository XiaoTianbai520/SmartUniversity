package com.smart.university.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 选课结果消息，用于异步刷新容量缓存、生成课表快照等下游处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSelectionMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 选课记录 ID
     */
    private Long selectionId;

    /**
     * 学生 ID
     */
    private Long studentId;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 选课批次 ID
     */
    private Long batchId;

    /**
     * 操作类型：SELECT 选课 / WITHDRAW 退课
     */
    private String operateType;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
}
