package com.smart.university.web.annotation;

import com.smart.university.common.enums.RoleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解，标注在 Controller 方法或类上，由鉴权拦截器统一校验
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色列表，为空表示只需登录
     *
     * @return 角色列表
     */
    RoleEnum[] value();
}
