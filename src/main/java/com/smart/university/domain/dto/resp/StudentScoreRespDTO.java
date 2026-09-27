package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生成绩出参
 */
@Data
public class StudentScoreRespDTO {

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 成绩
     */
    private BigDecimal score;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;
}
