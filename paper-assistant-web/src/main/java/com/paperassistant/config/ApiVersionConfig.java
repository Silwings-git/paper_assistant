package com.paperassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 版本前缀配置，所有 REST API 使用 /api/v1
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // API 路径由 Controller 的 @RequestMapping 显式指定 /api/v1
    }
}
