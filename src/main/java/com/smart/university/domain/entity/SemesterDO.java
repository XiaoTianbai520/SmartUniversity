package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.smart.university.domain.enums.SemesterStatusEnum;

/**
 * semester 表
 */
@Data
@TableName("semester")
public class SemesterDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
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
