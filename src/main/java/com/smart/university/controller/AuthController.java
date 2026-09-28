package com.smart.university.controller;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.LoginReqDTO;
import com.smart.university.domain.dto.resp.CurrentUserRespDTO;
import com.smart.university.domain.dto.resp.LoginRespDTO;
import com.smart.university.service.AuthService;
import com.smart.university.web.annotation.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证接口
 */
@Tag(name = "登录认证", description = "账号登录、获取当前登录用户与退出登录")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录
     *
     * @param requestParam 登录入参
     * @return 登录结果
     */
    @Operation(summary = "登录", description = "账号密码登录，返回 Token，无需鉴权即可访问")
    @SecurityRequirements(value = {})
    @PostMapping("/login")
    public Result<LoginRespDTO> login(@Valid @RequestBody LoginReqDTO requestParam) {
        return Result.success("登录成功", authService.login(requestParam));
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户信息
     */
    @Operation(summary = "获取当前登录用户", description = "返回当前 Token 对应的登录用户信息")
    @GetMapping("/me")
    @RequireRole({RoleEnum.STUDENT, RoleEnum.TEACHER, RoleEnum.ADMIN})
    public Result<CurrentUserRespDTO> getCurrentUser() {
        return Result.success(authService.getCurrentUser());
    }

    /**
     * 退出登录
     *
     * @return 空响应
     */
    @Operation(summary = "退出登录", description = "注销当前登录态，使 Token 失效")
    @PostMapping("/logout")
    @RequireRole({RoleEnum.STUDENT, RoleEnum.TEACHER, RoleEnum.ADMIN})
    public Result<Void> logout() {
        authService.logout();
        return Result.success("退出成功", null);
    }
}
