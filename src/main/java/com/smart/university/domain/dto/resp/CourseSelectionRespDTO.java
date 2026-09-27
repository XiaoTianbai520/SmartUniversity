package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 选课结果出参
 */
@Data
public class CourseSelectionRespDTO {

    /**
     * 选课记录 ID
     */
    private Long selectionId;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 选课状态
     */
    private String status;

    /**
     * 选课时间
     */
    private LocalDateTime selectedAt;
}
