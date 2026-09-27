package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.smart.university.domain.enums.SemesterStatusEnum;

/**
 * 学期表
 */
@Data
public class SemesterDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 学期ID
     */
    private Long id;

    /**
     * 学期编号，例如2026-2027-1
     */
    private String semesterCode;

    /**
     * 学年，例如2026-2027
     */
    private String academicYear;

    /**
     * 学期序号
     */
    private Integer termNo;

    /**
     * 学期开始日期
     */
    private LocalDate startDate;

    /**
     * 学期结束日期
     */
    private LocalDate endDate;

    /**
     * 学生本学期最大可选学分
     */
    private BigDecimal maxSelectionCredit;

    /**
     * PLANNED/ACTIVE/FINISHED
     */
    private SemesterStatusEnum status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
