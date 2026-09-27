package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * 教学班适用年级表
 */
@Data
public class TeachingClassGradeDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 教学班ID
     */
    private Long teachingClassId;

    /**
     * 适用年级ID
     */
    private Long gradeId;
}
