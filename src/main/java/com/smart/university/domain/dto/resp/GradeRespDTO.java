package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 年级出参
 */
@Data
public class GradeRespDTO {

    /**
     * 年级 ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 入学年份
     */
    private Integer entryYear;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;
}
