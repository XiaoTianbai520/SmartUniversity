package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.university.common.enums.RiskLevelEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * data_audit_issue 表，异常数据巡检结果，只读不改业务数据
 */
@Data
@TableName("data_audit_issue")
public class DataAuditIssueDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 问题ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 巡检规则编码，例如 CAPACITY_ZERO / SCHEDULE_CONFLICT
     */
    private String ruleCode;

    /**
     * 规则中文名
     */
    private String ruleName;

    /**
     * 业务类型：TEACHING_CLASS/CLASSROOM/TEACHER/SCORE/SEMESTER
     */
    private String bizType;

    /**
     * 关联业务ID
     */
    private Long bizId;

    /**
     * 严重级别：LOW/MEDIUM/HIGH
     */
    private RiskLevelEnum severity;

    /**
     * 问题描述
     */
    private String description;

    /**
     * 所属学期ID，可为空
     */
    private Long semesterId;

    /**
     * 巡检日期
     */
    private LocalDate statDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
