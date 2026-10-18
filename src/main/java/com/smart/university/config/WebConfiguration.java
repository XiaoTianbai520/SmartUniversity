package com.smart.university.config;

import cn.hutool.core.util.StrUtil;
import com.smart.university.web.interceptor.AuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Web 层配置，注册鉴权拦截器并声明免鉴权路径
 */
@EnableAsync
@EnableScheduling
@Configuration
@RequiredArgsConstructor
public class WebConfiguration implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    /**
     * 成绩发布后自动统计的专用线程池，与 Web 请求线程隔离，避免统计刷新阻塞接口响应
     *
     * @return 成绩统计异步线程池
     */
    @Bean("scoreStatisticsExecutor")
    public TaskExecutor scoreStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("score-stat-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

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

    /**
     * 全局跨域配置：放行跨域来源（含前端开发服务器），使浏览器预检请求 OPTIONS 不再被 Spring MVC 直接拒绝
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
