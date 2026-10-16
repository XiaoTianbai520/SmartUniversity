package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教师工作量按教学班的明细出参（39.1 classes）
 */
@Data
public class TeacherWorkloadClassRespDTO {

    /**
     * 教学班 ID
     */
    @Schema(example = "1")
    private Long teachingClassId;

    /**
     * 教学班编号
     */
    private String classCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程学分
     */
    private BigDecimal credit;

    /**
     * 教学班容量
     */
    private Integer capacity;

    /**
     * 已选人数（DB 权威口径，C1）
     */
    private Integer selectedCount;

    /**
     * 该教学班课时（节次 × 周数之和）
     */
    private Integer totalHours;
}
