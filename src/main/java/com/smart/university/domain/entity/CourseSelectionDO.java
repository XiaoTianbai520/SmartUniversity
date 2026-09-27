package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.smart.university.domain.enums.SelectionStatusEnum;

/**
 * course_selection 表
 */
@Data
@TableName("course_selection")
public class CourseSelectionDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 教学班ID
     */
    private Long teachingClassId;

    /**
     * 选课批次ID
     */
    private Long batchId;

    /**
     * SELECTED/WITHDRAWN
     */
    private SelectionStatusEnum status;

    /**
     * 最近一次选课时间
     */
    private LocalDateTime selectedAt;

    /**
     * 最近一次退课时间
     */
    private LocalDateTime withdrawnAt;

    /**
     * 首次创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
