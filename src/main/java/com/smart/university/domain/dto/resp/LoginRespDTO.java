package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 登录出参
 */
@Data
public class LoginRespDTO {

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 角色
     */
    private String role;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 展示名称
     */
    private String displayName;
}
