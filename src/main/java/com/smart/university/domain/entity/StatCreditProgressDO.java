package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.university.common.enums.RiskLevelEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * stat_credit_progress 表，学生学分进度统计快照（跨学期累计，C8）
 */
@Data
@TableName("stat_credit_progress")
public class StatCreditProgressDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 快照ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学期ID，仅表示快照所属学期，不代表学分统计范围
     */
    private Long semesterId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学号快照
     */
    private String studentNo;

    /**
     * 学生姓名快照
     */
    private String studentName;

    /**
     * 所属专业ID快照
     */
    private Long majorId;

    /**
     * 所属年级ID快照
     */
    private Long gradeId;

    /**
     * 是否存在培养方案：1有，0无
     */
    private Integer hasPlan;

    /**
     * 已完成总学分（跨学期累计，已发布即计入，不判及格）
     */
    private BigDecimal completedCredit;

    /**
     * 及格学分（跨学期累计，附加展示字段，不参与完成度与风险计算）
     */
    private BigDecimal passedCredit;

    /**
     * 已完成的必修学分
     */
    private BigDecimal requiredCredit;

    /**
     * 已完成的专业选修学分
     */
    private BigDecimal majorElectiveCredit;

    /**
     * 已完成的公共选修学分
     */
    private BigDecimal publicElectiveCredit;

    /**
     * 培养方案毕业总学分要求
     */
    private BigDecimal planTotalCredit;

    /**
     * 培养方案必修学分要求
     */
    private BigDecimal planRequiredCredit;

    /**
     * 培养方案专业选修学分要求
     */
    private BigDecimal planMajorElectiveCredit;

    /**
     * 培养方案公共选修学分要求
     */
    private BigDecimal planPublicElectiveCredit;

    /**
     * 总体完成度百分比，0~100
     */
    private BigDecimal progressRate;

    /**
     * 毕业风险等级：LOW/MEDIUM/HIGH
     */
    private RiskLevelEnum riskLevel;

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
