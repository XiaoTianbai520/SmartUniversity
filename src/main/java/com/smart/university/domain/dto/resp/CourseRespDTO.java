package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 课程出参
 */
@Data
public class CourseRespDTO {

    /**
     * 课程 ID
     */
    private Long courseId;

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
     * 学分
     */
    private BigDecimal credit;

    /**
     * 课程性质
     */
    private String courseType;

    /**
     * 默认教学班容量
     */
    private Integer defaultCapacity;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;
}
