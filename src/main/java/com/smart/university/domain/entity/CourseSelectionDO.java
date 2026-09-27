package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.smart.university.domain.enums.SelectionStatusEnum;

/**
 * 学生选课记录表
 */
@Data
public class CourseSelectionDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 选课记录ID
     */
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
