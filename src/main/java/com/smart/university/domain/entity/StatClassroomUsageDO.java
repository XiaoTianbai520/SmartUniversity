package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * stat_classroom_usage 表，教室利用率统计快照
 */
@Data
@TableName("stat_classroom_usage")
public class StatClassroomUsageDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 快照ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 教室ID
     */
    private Long classroomId;

    /**
     * 教学楼名称快照
     */
    private String buildingName;

    /**
     * 教室编号快照
     */
    private String roomNo;

    /**
     * 教室容量快照
     */
    private Integer capacity;

    /**
     * 使用次数（排课条数）
     */
    private Integer usageCount;

    /**
     * 使用课时（节次×周数之和）
     */
    private Integer usageHours;

    /**
     * 可排课时总数（分母）
     */
    private Integer totalHours;

    /**
     * 利用率百分比，0~100
     */
    private BigDecimal usageRate;

    /**
     * 快照日期
     */
    private LocalDate statDate;

    /**
     * 快照生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
