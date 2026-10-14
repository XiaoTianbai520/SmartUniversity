package com.smart.university.domain.dto.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
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
     * 成绩；未录入成绩的学生显式返回 null（覆盖全局 non_null，保证与接口文档 §7.5 一致）
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private BigDecimal score;

    /**
     * UNPUBLISHED / PUBLISHED；未录入成绩的学生显式返回 null（覆盖全局 non_null）
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String status;
}
