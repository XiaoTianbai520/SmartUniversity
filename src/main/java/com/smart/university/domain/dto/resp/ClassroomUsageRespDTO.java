package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教室利用率出参（37.1）
 *
 * <p>dimension=CLASSROOM / BUILDING 时返回教室 / 教学楼维度的利用率；
 * WEEKDAY_SECTION 时返回星期 + 节次维度的使用次数；
 * FREE_SLOT 时返回未被排课覆盖的教室 + 星期 + 节次组合。
 */
@Data
public class ClassroomUsageRespDTO {

    /**
     * 教室 ID，BUILDING / WEEKDAY_SECTION 维度为空
     */
    @Schema(example = "1")
    private Long classroomId;

    /**
     * 教学楼名称
     */
    private String buildingName;

    /**
     * 教室编号
     */
    private String roomNo;

    /**
     * 教室容量
     */
    @Schema(example = "120")
    private Integer capacity;

    /**
     * 使用次数（排课条数）
     */
    private Integer usageCount;

    /**
     * 使用课时（节次 × 周数之和）
     */
    private Integer usageHours;

    /**
     * 可排课时总数（分母）
     */
    private Integer totalHours;

    /**
     * 利用率百分比，0~100，保留 1 位小数，不拼 % 符号
     */
    private BigDecimal usageRate;

    /**
     * 星期：1周一...7周日，仅 WEEKDAY_SECTION / FREE_SLOT 维度返回
     */
    private Integer weekday;

    /**
     * 节次，仅 WEEKDAY_SECTION / FREE_SLOT 维度返回
     */
    private Integer section;
}
