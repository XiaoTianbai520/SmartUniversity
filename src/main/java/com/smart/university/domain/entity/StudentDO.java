package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生表
 */
@Data
public class StudentDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 学生ID
     */
    private Long id;

    /**
     * 对应系统用户ID
     */
    private Long userId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 所属专业ID
     */
    private Long majorId;

    /**
     * 所属年级ID
     */
    private Long gradeId;

    /**
     * 所属行政班级ID
     */
    private Long classId;

    /**
     * 学生状态：1正常，0停用
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
