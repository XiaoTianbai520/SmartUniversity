package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 行政班分页查询入参
 */
@Data
public class AcademicClassPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 班级编号 / 名称关键字
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
     * 状态：1 启用，0 禁用
     */
    private Integer status;
}
