package com.smart.university.common.exception;

import com.smart.university.common.enums.ResultCodeEnum;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常，通过业务错误码透出给前端
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码
     */
    private final int code;

    public BizException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    public BizException(ResultCodeEnum resultCodeEnum, String message) {
        super(message);
        this.code = resultCodeEnum.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
