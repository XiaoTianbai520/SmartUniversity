package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 教师简要出参
 */
@Data
public class TeacherBriefRespDTO {

    /**
     * 教师 ID
     */
    private Long teacherId;

    /**
     * 教师姓名
     */
    private String teacherName;
}
