package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * classroom 表
 */
@Data
@TableName("classroom")
public class ClassroomDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 教学楼名称
     */
    private String buildingName;

    /**
     * 教室编号
     */
    private String roomNo;

    /**
     * 教室容量
     */
    private Integer capacity;

    /**
     * 状态：1可用，0停用
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
