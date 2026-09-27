package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 排课出参
 */
@Data
public class ScheduleRespDTO {

    /**
     * 星期 1-7
     */
    private Integer weekday;

    /**
     * 开始节次
     */
    private Integer startSection;

    /**
     * 结束节次
     */
    private Integer endSection;

    /**
     * 开始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;

    /**
     * 教学楼名称
     */
    private String buildingName;

    /**
     * 教室编号
     */
    private String roomNo;
}
