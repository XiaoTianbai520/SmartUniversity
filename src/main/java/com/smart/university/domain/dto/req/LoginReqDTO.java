package com.smart.university.domain.dto.req;

import lombok.Data;
import com.smart.university.common.enums.RoleEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录入参
 */
@Data
public class LoginReqDTO {

    /**
     * 登录账号
     */
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /**
     * 登录密码
     */
    @NotBlank(message = "登录密码不能为空")
    private String password;

    /**
     * 登录入口角色：STUDENT/TEACHER/ADMIN
     */
    @NotNull(message = "登录类型不能为空")
    private RoleEnum loginType;
}
