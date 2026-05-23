package com.earth.online.player.ailearn.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * traceId 请求过滤器。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * 为每个请求生成或透传 traceId。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        TraceContext.setTraceId(traceId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }

    /**
     * 解析请求中的 traceId，未传入时自动生成。
     *
     * @param request HTTP 请求
     * @return 请求链路标识
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TraceContext.TRACE_ID_HEADER);
        if (StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
