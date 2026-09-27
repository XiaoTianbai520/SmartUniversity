package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 学生分页查询入参
 */
@Data
public class StudentPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学号 / 姓名关键字
     */
    private String keyword;

    /**
     * 专业 ID
     */
    private Long majorId;

    /**
     * 年级 ID
     */
    private Long gradeId;

    /**
     * 行政班级 ID
     */
    private Long classId;

    /**
     * 状态：1 正常，0 停用
     */
    private Integer status;
}
