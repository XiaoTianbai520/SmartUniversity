package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教室利用率口径回显（37.1 extra），决策 5 要求必须回显分母口径
 */
@Data
public class ClassroomUsageExtraRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 每周计天数
     */
    @Schema(example = "5")
    private Integer daysPerWeek;

    /**
     * 每天计节次
     */
    @Schema(example = "10")
    private Integer sectionsPerDay;

    /**
     * 学期周数，由起止日期推导并取上限
     */
    @Schema(example = "20")
    private Integer semesterWeeks;

    /**
     * 参与统计的教室数
     */
    @Schema(example = "42")
    private Integer classroomCount;

    /**
     * 整体利用率百分比，保留 1 位小数
     */
    private BigDecimal overallUsageRate;

    /**
     * 快照生成时间，无快照时为当前时间
     */
    private LocalDateTime snapshotTime;
}
