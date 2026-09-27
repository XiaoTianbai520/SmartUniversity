package com.smart.university.common.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.smart.university.common.enums.RoleEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具，负责登录令牌的签发与解析
 */
@Component
public class JwtUtil {

    /**
     * 载荷：用户 ID
     */
    public static final String PAYLOAD_USER_ID = "userId";

    /**
     * 载荷：角色
     */
    public static final String PAYLOAD_ROLE = "role";

    /**
     * 载荷：登录账号
     */
    public static final String PAYLOAD_USERNAME = "username";

    /**
     * 签名密钥
     */
    @Value("${smart-university.auth.jwt-secret}")
    private String jwtSecret;

    /**
     * Token 有效期（秒）
     */
    @Value("${smart-university.auth.token-ttl}")
    private long tokenTtl;

    /**
     * 签发 Token
     *
     * @param userId   用户 ID
     * @param username 登录账号
     * @param role     角色
     * @return Token
     */
    public String generateToken(Long userId, String username, RoleEnum role) {
        return JWT.create()
                .setPayload(PAYLOAD_USER_ID, userId)
                .setPayload(PAYLOAD_USERNAME, username)
                .setPayload(PAYLOAD_ROLE, role.name())
                .setExpiresAt(new Date(System.currentTimeMillis() + tokenTtl * 1000))
                .setKey(getSecretKeyBytes())
                .sign();
    }

    /**
     * 解析 Token，返回校验通过的 JWT 对象
     *
     * @param token Token
     * @return JWT 对象，校验失败返回 null
     */
    public JWT parseToken(String token) {
        JWT result = JWTUtil.parseToken(token).setKey(getSecretKeyBytes());
        return result.validate(0) ? result : null;
    }

    /**
     * 从 Token 中解析用户 ID
     *
     * @param token Token
     * @return 用户 ID，解析失败返回 null
     */
    public Long getUserId(String token) {
        JWT jwt = parseToken(token);
        if (jwt == null) {
            return null;
        }
        return jwt.getPayloads().getLong(PAYLOAD_USER_ID);
    }

    /**
     * 从 Token 中解析角色
     *
     * @param token Token
     * @return 角色，解析失败返回 null
     */
    public RoleEnum getRole(String token) {
        JWT jwt = parseToken(token);
        if (jwt == null) {
            return null;
        }
        return RoleEnum.getByCode(jwt.getPayloads().getStr(PAYLOAD_ROLE));
    }

    private byte[] getSecretKeyBytes() {
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }
}
