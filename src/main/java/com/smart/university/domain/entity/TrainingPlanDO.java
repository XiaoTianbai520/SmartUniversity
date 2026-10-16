package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * training_plan 表，培养方案：专业 + 入学年级的毕业学分要求
 */
@Data
@TableName("training_plan")
public class TrainingPlanDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 培养方案ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 培养方案名称
     */
    private String planName;

    /**
     * 所属专业ID
     */
    private Long majorId;

    /**
     * 所属年级ID（入学年级）
     */
    private Long gradeId;

    /**
     * 毕业总学分要求
     */
    private BigDecimal totalCredit;

    /**
     * 必修学分要求
     */
    private BigDecimal requiredCredit;

    /**
     * 专业选修学分要求
     */
    private BigDecimal majorElectiveCredit;

    /**
     * 公共选修学分要求
     */
    private BigDecimal publicElectiveCredit;

    /**
     * 状态：1启用，0停用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
