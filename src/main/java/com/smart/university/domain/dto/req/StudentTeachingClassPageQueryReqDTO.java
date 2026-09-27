package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 学生可选课程分页查询入参
 */
@Data
public class StudentTeachingClassPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 课程名称 / 教师名称关键字
     */
    private String keyword;

    /**
     * 课程性质
     */
    private String courseType;

    /**
     * 星期 1-7
     */
    private Integer weekday;

    /**
     * 当前学生 ID，由后端填充
     */
    private Long studentId;

    /**
     * 当前学生专业 ID，由后端填充
     */
    private Long majorId;

    /**
     * 当前学生年级 ID，由后端填充
     */
    private Long gradeId;

    /**
     * 当前学期 ID，由后端填充
     */
    private Long semesterId;

    /**
     * 当前有效选课批次 ID，由后端填充
     */
    private Long batchId;
}
