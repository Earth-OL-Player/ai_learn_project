package com.earth.online.player.ailearn.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 基础配置。
 */
@Configuration
public class WebConfig {

    /**
     * 配置本地开发跨域访问。
     *
     * @return Web MVC 配置器
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * 添加跨域映射，支持本地前端联调。
             *
             * @param registry 跨域注册器
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/**")
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Trace-Id")
                        .maxAge(3600);
            }
        };
    }
}
