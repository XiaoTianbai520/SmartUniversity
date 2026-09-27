package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.smart.university.domain.enums.CourseTypeEnum;

/**
 * course 表
 */
@Data
@TableName("course")
public class CourseDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
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
