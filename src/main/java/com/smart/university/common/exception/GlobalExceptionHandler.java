package com.smart.university.common.exception;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器，统一兜底并转换为统一响应结构
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param ex 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        log.warn("业务异常，code：{}，message：{}", ex.getCode(), ex.getMessage());
        return Result.failure(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数绑定与校验异常
     *
     * @param ex 参数异常
     * @return 统一响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse(ResultCodeEnum.PARAM_ERROR.getMessage());
        return Result.failure(ResultCodeEnum.PARAM_ERROR, message);
    }

    /**
     * 处理请求体解析失败与参数缺失异常
     *
     * @param ex 请求异常
     * @return 统一响应
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public Result<Void> handleRequestException(Exception ex) {
        log.warn("请求参数异常：{}", ex.getMessage());
        return Result.failure(ResultCodeEnum.FORMAT_ERROR);
    }

    /**
     * 处理数据库唯一键冲突，转换为数据已存在，避免直接抛出系统异常
     *
     * @param ex 唯一键冲突异常
     * @return 统一响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException ex) {
        log.warn("唯一键冲突：{}", ex.getRootCause() == null ? ex.getMessage() : ex.getRootCause().getMessage());
        return Result.failure(ResultCodeEnum.DATA_ALREADY_EXIST);
    }

    /**
     * 处理未预期的系统异常
     *
     * @param ex 系统异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return Result.failure(ResultCodeEnum.SYSTEM_ERROR);
    }
}
