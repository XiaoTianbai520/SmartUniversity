package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * selection_batch_class 表
 */
@Data
@TableName("selection_batch_class")
public class SelectionBatchClassDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
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
