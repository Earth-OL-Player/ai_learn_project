package com.earth.online.player.ailearn.common.config;

import com.earth.online.player.ailearn.ai.AiServiceProperties;
import com.earth.online.player.ailearn.common.security.JwtProperties;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Web 基础配置。
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AiServiceProperties.class})
public class WebConfig {

    private static final String API_PATH_PATTERN = "/api/v1/**";
    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    /**
     * 配置本地开发跨域访问过滤器。
     *
     * @return CORS 过滤器注册信息
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Vite 本地端口可能被占用后自动切换，按 localhost/127.0.0.1 放开任意本地端口。
        configuration.setAllowedOriginPatterns(List.of("http://localhost:[*]", "http://127.0.0.1:[*]"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        // 暴露追踪和续期头，便于前端统一处理登录态。
        configuration.setExposedHeaders(List.of("X-Trace-Id", "X-Refresh-Token"));
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(API_PATH_PATTERN, configuration);

        // CORS 必须早于认证过滤器执行，避免预检或认证失败响应缺少跨域响应头。
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>(new CorsFilter(source));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * 创建密码编码器。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
