package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教学班排课表
 */
@Data
public class TeachingClassScheduleDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 排课ID
     */
    private Long id;

    /**
     * 教学班ID
     */
    private Long teachingClassId;

    /**
     * 教室ID
     */
    private Long classroomId;

    /**
     * 星期：1周一...7周日
     */
    private Integer weekday;

    /**
     * 开始节次
     */
    private Integer startSection;

    /**
     * 结束节次
     */
    private Integer endSection;

    /**
     * 开始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
