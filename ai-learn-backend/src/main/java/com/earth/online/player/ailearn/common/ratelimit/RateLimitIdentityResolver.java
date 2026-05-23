package com.earth.online.player.ailearn.common.ratelimit;

import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 限流身份标识解析器。
 */
@Component
public class RateLimitIdentityResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String REAL_IP_HEADER = "X-Real-IP";
    private static final String IP_PREFIX = "ip:";
    private static final String USER_PREFIX = "user:";
    private static final String UNKNOWN_IP = "unknown";

    /**
     * 解析客户端 IP 限流键。
     *
     * @param request HTTP 请求
     * @return IP 限流键
     */
    public String resolveIpKey(HttpServletRequest request) {
        return IP_PREFIX + resolveClientIp(request);
    }

    /**
     * 解析当前登录用户限流键。
     *
     * @return 用户限流键，未登录时返回空
     */
    public String resolveUserKey() {
        AuthenticatedUser user = AuthContext.getUser();
        return resolveUserKey(user);
    }

    /**
     * 解析指定登录用户限流键。
     *
     * @param user 登录用户
     * @return 用户限流键，未登录时返回空
     */
    public String resolveUserKey(AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            return "";
        }
        return USER_PREFIX + user.userId();
    }

    /**
     * 解析客户端 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        // 反向代理未传递 X-Forwarded-For 时，使用常见真实 IP 头兜底。
        String realIp = request.getHeader(REAL_IP_HEADER);
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : UNKNOWN_IP;
    }
}
