package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 课程分页查询入参
 */
@Data
public class CoursePageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 课程编号 / 名称关键字
     */
    private String keyword;

    /**
     * 课程性质
     */
    private String courseType;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;
}
