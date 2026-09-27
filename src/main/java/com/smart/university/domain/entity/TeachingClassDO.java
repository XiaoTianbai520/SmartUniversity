package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.smart.university.domain.enums.TeachingClassStatusEnum;

/**
 * teaching_class 表
 */
@Data
@TableName("teaching_class")
public class TeachingClassDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
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
