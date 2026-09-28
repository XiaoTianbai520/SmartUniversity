package com.smart.university.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j / OpenAPI 文档配置，聚合学生端 / 教师端 / 管理端全部接口，便于联调调试
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * 安全方案名称，与全局鉴权请求头 {@code Authorization: Bearer <token>} 对应
     */
    private static final String SECURITY_SCHEME_NAME = "Authorization";

    /**
     * 构建 OpenAPI 文档元数据与全局安全方案
     *
     * @return OpenAPI 实例
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("高校智慧教务选课平台 API")
                        .description("V1 接口文档，覆盖认证、学生端、教师端、管理端与通知等全部接口，供前端联调与调试使用")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SmartUniversity")
                                .email("c13383413150@163.com")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name(SECURITY_SCHEME_NAME)
                                .description("登录后返回的 Token，格式：Bearer <token>")))
                // 全局安全方案：在 Knife4j 的 Authorize 中配置一次，即可对所有接口生效
                .security(List.of(new SecurityRequirement().addList(SECURITY_SCHEME_NAME)));
    }
}
