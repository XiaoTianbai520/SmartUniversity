package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 教学班成绩列表项出参
 */
@Data
public class TeacherScoreRespDTO {

    /**
     * 选课记录 ID
     */
    private Long selectionId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 成绩
     */
    private BigDecimal score;

    /**
     * UNPUBLISHED / PUBLISHED
     */
    private String status;
}
