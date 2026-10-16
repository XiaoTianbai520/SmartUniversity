package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 教师本人工作量出参（39.1）
 */
@Data
public class TeacherWorkloadDetailRespDTO {

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
     * 学期 ID
     */
    private Long semesterId;

    /**
     * 教学班数
     */
    private Integer classCount;

    /**
     * 总课时
     */
    private Integer totalHours;

    /**
     * 授课学生数（去重）
     */
    private Integer studentCount;

    /**
     * 按教学班的明细
     */
    private List<TeacherWorkloadClassRespDTO> classes;
}
