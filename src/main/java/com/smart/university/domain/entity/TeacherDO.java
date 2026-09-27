package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教师表
 */
@Data
public class TeacherDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教师ID
     */
    private Long id;

    /**
     * 对应系统用户ID
     */
    private Long userId;

    /**
     * 教师编号
     */
    private String teacherNo;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 职称
     */
    private String title;

    /**
     * 教师状态：1正常，0停用
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
