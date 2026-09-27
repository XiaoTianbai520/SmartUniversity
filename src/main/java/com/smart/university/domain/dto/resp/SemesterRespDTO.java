package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 学期出参
 */
@Data
public class SemesterRespDTO {

    /**
     * 学期 ID
     */
    private Long semesterId;

    /**
     * 学期编号
     */
    private String semesterCode;

    /**
     * 学年
     */
    private String academicYear;

    /**
     * 学期序号
     */
    private Integer termNo;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 最大可选学分
     */
    private BigDecimal maxSelectionCredit;

    /**
     * PLANNED / ACTIVE / FINISHED
     */
    private String status;
}
