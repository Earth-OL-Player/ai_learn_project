package com.earth.online.player.ailearn.common.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口。
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /**
     * 查询应用健康状态。
     *
     * @return 健康状态响应
     */
    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> healthInfo = Map.of(
                "status", "UP",
                "service", "ai-learn-backend",
                "time", OffsetDateTime.now().toString()
        );
        return ApiResponse.success(healthInfo);
    }
}
