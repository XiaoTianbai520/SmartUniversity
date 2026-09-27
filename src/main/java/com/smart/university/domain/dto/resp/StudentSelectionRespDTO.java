package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 学生我的课程出参
 */
@Data
public class StudentSelectionRespDTO {

    /**
     * 选课记录 ID
     */
    private Long selectionId;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 选课状态
     */
    private String status;

    /**
     * 选课时间
     */
    private LocalDateTime selectedAt;

    /**
     * 排课信息
     */
    private List<ScheduleRespDTO> schedules;
}
