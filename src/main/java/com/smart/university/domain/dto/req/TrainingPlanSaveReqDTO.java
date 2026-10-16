package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 培养方案新增 / 修改入参
 */
@Data
public class TrainingPlanSaveReqDTO {

    /**
     * 培养方案 ID，修改时由路径参数回填
     */
    @Schema(example = "1")
    private Long id;

    /**
     * 培养方案名称
     */
    @Schema(example = "软件工程2026级培养方案")
    @NotBlank(message = "培养方案名称不能为空")
    @Size(max = 100, message = "培养方案名称长度不能超过 100")
    private String planName;

    /**
     * 所属专业 ID
     */
    @Schema(example = "1")
    @NotNull(message = "专业不能为空")
    private Long majorId;

    /**
     * 所属年级 ID（入学年级）
     */
    @Schema(example = "1")
    @NotNull(message = "年级不能为空")
    private Long gradeId;

    /**
     * 毕业总学分要求
     */
    @Schema(example = "160.0")
    @NotNull(message = "毕业总学分不能为空")
    @DecimalMin(value = "0.1", message = "毕业总学分必须大于 0")
    private BigDecimal totalCredit;

    /**
     * 必修学分要求
     */
    @Schema(example = "120.0")
    private BigDecimal requiredCredit;

    /**
     * 专业选修学分要求
     */
    @Schema(example = "25.0")
    private BigDecimal majorElectiveCredit;

    /**
     * 公共选修学分要求
     */
    @Schema(example = "15.0")
    private BigDecimal publicElectiveCredit;

    /**
     * 状态：1 启用，0 停用
     */
    @Schema(example = "1")
    private Integer status;

    /**
     * 备注
     */
    @Schema(example = "")
    private String remark;
}
