package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行政班级表
 */
@Data
public class AcademicClassDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 班级ID
     */
    private Long id;

    /**
     * 班级编号
     */
    private String classCode;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 所属专业ID
     */
    private Long majorId;

    /**
     * 所属年级ID
     */
    private Long gradeId;

    /**
     * 状态：1启用，0禁用
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
