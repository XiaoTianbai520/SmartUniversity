package com.smart.university.common.base;

import com.smart.university.common.enums.ResultCodeEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 前后端统一响应结构
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码，成功固定为 200
     */
    private int code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 时间戳，便于前端排查问题
     */
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造成功响应
     *
     * @param data 业务数据
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    /**
     * 构造成功响应并自定义提示信息
     *
     * @param message 提示信息
     * @param data    业务数据
     * @return 成功响应
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), message, data);
    }

    /**
     * 构造成功响应，无业务数据
     *
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 构造失败响应
     *
     * @param resultCodeEnum 业务错误码枚举
     * @return 失败响应
     */
    public static <T> Result<T> failure(ResultCodeEnum resultCodeEnum) {
        return new Result<>(resultCodeEnum.getCode(), resultCodeEnum.getMessage(), null);
    }

    /**
     * 构造失败响应并覆盖提示信息
     *
     * @param resultCodeEnum 业务错误码枚举
     * @param message        覆盖后的提示信息
     * @return 失败响应
     */
    public static <T> Result<T> failure(ResultCodeEnum resultCodeEnum, String message) {
        return new Result<>(resultCodeEnum.getCode(), message, null);
    }

    /**
     * 构造失败响应
     *
     * @param code    业务错误码
     * @param message 提示信息
     * @return 失败响应
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断当前响应是否为成功响应
     *
     * @return 成功返回 true
     */
    public boolean isSuccess() {
        return ResultCodeEnum.SUCCESS.getCode() == this.code;
    }
}
