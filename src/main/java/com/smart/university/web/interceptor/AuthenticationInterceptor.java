package com.smart.university.web.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.smart.university.common.context.UserContext;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.JwtUtil;
import com.smart.university.common.util.RedisKeyUtil;
import com.smart.university.web.annotation.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 鉴权拦截器，解析 Token 还原登录身份，并校验接口角色权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    /**
     * Bearer 前缀
     */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        UserContext userContext = resolveUserContext(request);
        UserContextHolder.setUserContext(userContext);
        checkRole(handlerMethod, userContext);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    /**
     * 从请求头解析 Token 并还原登录用户上下文
     *
     * @param request 当前请求
     * @return 登录用户上下文
     */
    private UserContext resolveUserContext(HttpServletRequest request) {
        String token = resolveToken(request);
        if (StrUtil.isBlank(token)) {
            throw new BizException(ResultCodeEnum.NOT_LOGIN);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            throw new BizException(ResultCodeEnum.TOKEN_INVALID);
        }
        String cachedToken = stringRedisTemplate.opsForValue().get(RedisKeyUtil.buildLoginTokenKey(userId));
        if (StrUtil.isBlank(cachedToken) || !cachedToken.equals(token)) {
            throw new BizException(ResultCodeEnum.TOKEN_INVALID);
        }
        String cachedUser = stringRedisTemplate.opsForValue().get(RedisKeyUtil.buildLoginUserKey(userId));
        if (StrUtil.isBlank(cachedUser)) {
            throw new BizException(ResultCodeEnum.TOKEN_INVALID);
        }
        UserContext result = JSONUtil.toBean(cachedUser, UserContext.class);
        result.setToken(token);
        return result;
    }

    /**
     * 提取 Bearer Token
     *
     * @param request 当前请求
     * @return Token，不存在返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 校验当前登录用户是否具备接口要求的角色
     *
     * @param handlerMethod 目标处理方法
     * @param userContext   登录用户上下文
     */
    private void checkRole(HandlerMethod handlerMethod, UserContext userContext) {
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return;
        }
        boolean matched = Arrays.asList(requireRole.value()).contains(userContext.getRole());
        if (!matched) {
            throw new BizException(ResultCodeEnum.NO_API_PERMISSION);
        }
    }
}
