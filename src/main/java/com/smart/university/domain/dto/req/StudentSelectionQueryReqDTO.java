package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 学生我的课程查询入参
 */
@Data
public class StudentSelectionQueryReqDTO {

    /**
     * 学生 ID，由后端根据 Token 填充，禁止前端传入
     */
    private Long studentId;

    /**
     * 学期 ID，不传时使用当前学期
     */
    private Long semesterId;

    /**
     * 选课状态：SELECTED / WITHDRAWN
     */
    private String status;
}
