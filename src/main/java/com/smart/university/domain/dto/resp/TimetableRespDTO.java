package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 学生课表出参
 */
@Data
public class TimetableRespDTO {

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
     * 课程名称
     */
    private String courseName;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 教学楼名称
     */
    private String buildingName;

    /**
     * 教室编号
     */
    private String roomNo;
}
