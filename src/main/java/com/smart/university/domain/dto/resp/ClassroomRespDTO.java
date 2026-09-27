package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 教室出参
 */
@Data
public class ClassroomRespDTO {

    /**
     * 教室 ID
     */
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
    private Integer capacity;

    /**
     * 状态：1 可用，0 停用
     */
    private Integer status;
}
