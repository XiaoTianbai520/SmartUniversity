package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 教师查询教学班学生名单入参
 */
@Data
public class TeacherStudentPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学号 / 姓名关键字
     */
    private String keyword;
}
