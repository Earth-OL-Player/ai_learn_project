package com.earth.online.player.ailearn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 学习平台后端启动类。
 */
@MapperScan("com.earth.online.player.ailearn.user.infrastructure")
@SpringBootApplication
public class AiLearnBackendApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiLearnBackendApplication.class, args);
    }
}

