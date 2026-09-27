package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * 选课批次开放教学班关联表
 */
@Data
public class SelectionBatchClassDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 选课批次ID
     */
    private Long batchId;

    /**
     * 教学班ID
     */
    private Long teachingClassId;
}
