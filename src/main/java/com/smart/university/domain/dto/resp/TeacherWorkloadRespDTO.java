package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 教师工作量出参（37.2）
 */
@Data
public class TeacherWorkloadRespDTO {

    /**
     * 教师 ID
     */
    @Schema(example = "1")
    private Long teacherId;

    /**
     * 教师工号
     */
    private String teacherNo;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 教学班数
     */
    private Integer classCount;

    /**
     * 总课时（节次 × 周数之和，Q8 决议 A：1 节课 = 1 课时）
     */
    private Integer totalHours;

    /**
     * 授课学生数（去重）
     */
    private Integer studentCount;
}
