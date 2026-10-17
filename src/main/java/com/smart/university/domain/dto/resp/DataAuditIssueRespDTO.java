package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 异常数据巡检结果出参（42.2），只读结果，不对应任何业务数据变更
 */
@Data
public class DataAuditIssueRespDTO {

    /**
     * 问题 ID
     */
    @Schema(example = "1")
    private Long issueId;

    /**
     * 巡检规则编码
     */
    private String ruleCode;

    /**
     * 规则中文名
     */
    private String ruleName;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 关联业务 ID
     */
    private Long bizId;

    /**
     * 严重级别：LOW / MEDIUM / HIGH
     */
    private String severity;

    /**
     * 问题描述
     */
    private String description;

    /**
     * 所属学期 ID
     */
    private Long semesterId;

    /**
     * 巡检日期
     */
    private LocalDate statDate;
}
