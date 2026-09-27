package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 学期分页查询入参
 */
@Data
public class SemesterPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期编号关键字
     */
    private String keyword;

    /**
     * PLANNED / ACTIVE / FINISHED
     */
    private String status;
}
