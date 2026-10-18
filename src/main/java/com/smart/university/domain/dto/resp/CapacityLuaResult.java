package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * Lua 脚本返回值解析结果，统一承载业务码与上下文数值，
 * 禁止在 Service 里裸用 {@code List.get(0)}
 */
@Data
public class CapacityLuaResult {

    /**
     * 预扣成功
     */
    public static final int CODE_SUCCESS = 1;

    /**
     * 名额已满
     */
    public static final int CODE_FULL = 0;

    /**
     * 计数漂移，已用 DB 权威值校正
     */
    public static final int CODE_DRIFT = -1;

    /**
     * 业务码：1 成功 / 0 已满 / -1 漂移
     */
    private int code;

    /**
     * 脚本执行后的计数
     */
    private long count;

    /**
     * 教学班容量
     */
    private long capacity;

    /**
     * 解析脚本返回值，返回值为空或长度不足时按漂移处理
     *
     * @param scriptResult 脚本返回的 Long 列表
     * @return 解析结果
     */
    public static CapacityLuaResult parse(List<Long> scriptResult) {
        CapacityLuaResult result = new CapacityLuaResult();
        if (scriptResult == null || scriptResult.isEmpty()) {
            result.setCode(CODE_DRIFT);
            return result;
        }
        result.setCode(scriptResult.get(0) == null ? CODE_DRIFT : scriptResult.get(0).intValue());
        result.setCount(scriptResult.size() > 1 && scriptResult.get(1) != null ? scriptResult.get(1) : 0L);
        result.setCapacity(scriptResult.size() > 2 && scriptResult.get(2) != null ? scriptResult.get(2) : 0L);
        return result;
    }

    /**
     * 判断是否预扣成功
     *
     * @return 成功返回 true
     */
    public boolean isSuccess() {
        return CODE_SUCCESS == code;
    }

    /**
     * 判断是否名额已满
     *
     * @return 已满返回 true
     */
    public boolean isFull() {
        return CODE_FULL == code;
    }

    /**
     * 判断是否发生计数漂移
     *
     * @return 漂移返回 true
     */
    public boolean isDrift() {
        return CODE_DRIFT == code;
    }
}
