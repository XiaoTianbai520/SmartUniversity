package com.smart.university.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 风险等级枚举，取值与 stat_credit_progress.risk_level、data_audit_issue.severity 的 CHECK 约束一致
 */
@Getter
@AllArgsConstructor
public enum RiskLevelEnum {

    /**
     * 低风险
     */
    LOW("LOW", "低风险"),

    /**
     * 中风险
     */
    MEDIUM("MEDIUM", "中风险"),

    /**
     * 高风险
     */
    HIGH("HIGH", "高风险");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 风险等级描述
     */
    private final String description;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举，未匹配返回 null
     */
    public static RiskLevelEnum getByCode(String code) {
        for (RiskLevelEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
