package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 教学班分页查询入参
 */
@Data
public class TeachingClassPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID
     */
    private Long semesterId;

    /**
     * 课程 ID
     */
    private Long courseId;

    /**
     * 教师 ID
     */
    private Long teacherId;

    /**
     * DRAFT / AVAILABLE / CLOSED / CANCELLED
     */
    private String status;

    /**
     * 教学班编号 / 名称关键字
     */
    private String keyword;
}
