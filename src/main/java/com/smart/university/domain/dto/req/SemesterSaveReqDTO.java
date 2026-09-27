package com.smart.university.domain.dto.req;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 学期新增 / 修改入参
 */
@Data
public class SemesterSaveReqDTO {

    /**
     * 学期 ID，修改时必传
     */
    private Long id;

    /**
     * 学期编号，例如 2026-2027-1
     */
    @NotBlank(message = "学期编号不能为空")
    private String semesterCode;

    /**
     * 学年，例如 2026-2027
     */
    @NotBlank(message = "学年不能为空")
    private String academicYear;

    /**
     * 学期序号
     */
    @NotNull(message = "学期序号不能为空")
    private Integer termNo;

    /**
     * 学期开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 学期结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 本学期最大可选学分
     */
    @NotNull(message = "最大学分不能为空")
    private BigDecimal maxSelectionCredit;
}
