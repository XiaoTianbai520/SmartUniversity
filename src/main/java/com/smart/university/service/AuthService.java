package com.smart.university.service;

import com.smart.university.domain.dto.req.LoginReqDTO;
import com.smart.university.domain.dto.resp.CurrentUserRespDTO;
import com.smart.university.domain.dto.resp.LoginRespDTO;

/**
 * 登录认证服务
 */
public interface AuthService {

    /**
     * 登录，校验账号密码与登录入口角色后签发 Token
     *
     * @param requestParam 登录入参
     * @return 登录出参
     */
    LoginRespDTO login(LoginReqDTO requestParam);

    /**
     * 获取当前登录用户信息，身份一律从 Token 上下文获取
     *
     * @return 当前登录用户信息
     */
    CurrentUserRespDTO getCurrentUser();

    /**
     * 退出登录，失效当前 Token
     */
    void logout();
}
