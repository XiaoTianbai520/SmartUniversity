package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.smart.university.domain.enums.TeachingClassStatusEnum;

/**
 * 教学班表
 */
@Data
public class TeachingClassDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教学班ID
     */
    private Long id;

    /**
     * 教学班编号
     */
    private String classCode;

    /**
     * 教学班名称
     */
    private String className;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 任课教师ID
     */
    private Long teacherId;

    /**
     * 最大选课人数
     */
    private Integer capacity;

    /**
     * DRAFT/AVAILABLE/CLOSED/CANCELLED
     */
    private TeachingClassStatusEnum status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
