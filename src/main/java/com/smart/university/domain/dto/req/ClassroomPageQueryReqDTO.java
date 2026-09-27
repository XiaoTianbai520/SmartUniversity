package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 教室分页查询入参
 */
@Data
public class ClassroomPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 教学楼 / 教室编号关键字
     */
    private String keyword;

    /**
     * 状态：1 可用，0 停用
     */
    private Integer status;
}
