package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.smart.university.domain.enums.CourseTypeEnum;

/**
 * 课程基础信息表
 */
@Data
public class CourseDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 课程ID
     */
    private Long id;

    /**
     * 课程编号
     */
    private String courseCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程简介
     */
    private String description;

    /**
     * 课程学分
     */
    private BigDecimal credit;

    /**
     * REQUIRED/MAJOR_ELECTIVE/PUBLIC_ELECTIVE
     */
    private CourseTypeEnum courseType;

    /**
     * 默认教学班容量
     */
    private Integer defaultCapacity;

    /**
     * 状态：1启用，0停用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
