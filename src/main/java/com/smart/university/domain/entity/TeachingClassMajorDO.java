package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * teaching_class_major 表
 */
@Data
@TableName("teaching_class_major")
public class TeachingClassMajorDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 教学班ID
     */
    private Long teachingClassId;

    /**
     * 适用专业ID
     */
    private Long majorId;
}
