package com.earth.online.player.ailearn.ai;

import com.earth.online.player.ailearn.common.trace.TraceContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import org.springframework.util.StringUtils;

/**
 * AI 服务内部 HTTP 调用辅助工具。
 */
public final class AiServiceHttpSupport {

    /**
     * 工具类不允许实例化。
     */
    private AiServiceHttpSupport() {
    }

    /**
     * 创建固定 HTTP/1.1 的内部客户端。
     *
     * @param timeout 连接超时时间
     * @return HTTP 客户端
     */
    public static HttpClient newHttpClient(Duration timeout) {
        return HttpClient.newBuilder()
                .connectTimeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 创建 AI 服务内部请求构造器。
     *
     * @param properties AI 服务配置
     * @param timeout 请求超时时间
     * @param endpoint 完整请求地址
     * @return 已带内部认证头的请求构造器
     */
    public static HttpRequest.Builder newInternalRequestBuilder(
            AiServiceProperties properties,
            Duration timeout,
            String endpoint) {
        return addInternalHeaders(HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout)
                .version(HttpClient.Version.HTTP_1_1), properties);
    }

    /**
     * 补充 AI 服务内部调用通用请求头。
     *
     * @param builder 请求构造器
     * @param properties AI 服务配置
     * @return 请求构造器
     */
    public static HttpRequest.Builder addInternalHeaders(HttpRequest.Builder builder, AiServiceProperties properties) {
        builder.header(AiServiceConstants.INTERNAL_TOKEN_HEADER, properties.getToken());
        String traceId = TraceContext.getTraceId();
        if (StringUtils.hasText(traceId)) {
            builder.header(TraceContext.TRACE_ID_HEADER, traceId);
        }
        return builder;
    }

    /**
     * 规整 AI 服务基础地址。
     *
     * @param properties AI 服务配置
     * @return 无尾部斜杠的基础地址
     */
    public static String normalizeBaseUrl(AiServiceProperties properties) {
        return properties.getBaseUrl().replaceAll("/+$", "");
    }

    /**
     * 获取安全超时时间。
     *
     * @param properties AI 服务配置
     * @return 超时时间
     */
    public static Duration timeout(AiServiceProperties properties) {
        return Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()));
    }

    /**
     * 获取带上限的安全超时时间。
     *
     * @param properties AI 服务配置
     * @param maxTimeoutSeconds 最大超时时间秒数
     * @return 超时时间
     */
    public static Duration timeout(AiServiceProperties properties, int maxTimeoutSeconds) {
        int safeTimeoutSeconds = Math.min(maxTimeoutSeconds, Math.max(1, properties.getTimeoutSeconds()));
        return Duration.ofSeconds(safeTimeoutSeconds);
    }
}
