package com.earth.online.player.ailearn.common.config;

import com.earth.online.player.ailearn.ai.AiServiceProperties;
import com.earth.online.player.ailearn.common.ratelimit.RateLimitProperties;
import com.earth.online.player.ailearn.common.security.JwtProperties;
import com.earth.online.player.ailearn.model.domain.ModelAuthorizationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Web 基础配置。
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AiServiceProperties.class, RateLimitProperties.class, ModelAuthorizationProperties.class})
public class WebConfig {

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
