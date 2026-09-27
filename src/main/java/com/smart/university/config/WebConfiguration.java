package com.smart.university.config;

import cn.hutool.core.util.StrUtil;
import com.smart.university.web.interceptor.AuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 层配置，注册鉴权拦截器并声明免鉴权路径
 */
@Configuration
@RequiredArgsConstructor
public class WebConfiguration implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    /**
     * 免鉴权路径，多个使用英文逗号分隔
     */
    @Value("${smart-university.auth.exclude-paths:}")
    private String excludePaths;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePathList = StrUtil.isBlank(excludePaths)
                ? List.of()
                : StrUtil.splitTrim(excludePaths, ',');
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(excludePathList);
    }
}
