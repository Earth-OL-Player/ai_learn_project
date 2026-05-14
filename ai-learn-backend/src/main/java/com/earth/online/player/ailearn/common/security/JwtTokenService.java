package com.earth.online.player.ailearn.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT 令牌服务。
 */
@Component
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_TYPE = "JWT";

    private final JwtProperties jwtProperties;

    /**
     * 创建 JWT 令牌服务。
     *
     * @param jwtProperties JWT 配置
     */
    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成访问令牌。
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT 令牌
     */
    public String generateToken(Long userId, String username) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + jwtProperties.getExpiresInSeconds();

        // 使用稳定字段顺序，避免签名内容不一致。
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"" + TOKEN_TYPE + "\"}";
        String payloadJson = "{\"sub\":\"" + userId + "\",\"username\":\"" + escapeJson(username)
                + "\",\"iat\":" + now + ",\"exp\":" + expiresAt + "}";
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String unsignedToken = header + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    /**
     * 解析并校验 JWT 令牌。
     *
     * @param token JWT 令牌
     * @return 认证用户
     */
    public AuthenticatedUser parseToken(String token) {
        return parseTokenDetail(token).user();
    }

    /**
     * 解析并校验 JWT 令牌详情。
     *
     * @param token JWT 令牌
     * @return JWT 解析结果
     */
    public JwtParseResult parseTokenDetail(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JwtUnauthorizedException("登录状态已失效，请重新登录");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtUnauthorizedException("登录状态已失效，请重新登录");
        }

        // 固定时间比较签名，降低签名探测风险。
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new JwtUnauthorizedException("登录状态已失效，请重新登录");
        }

        Map<String, String> claims = parsePayload(parts[1]);
        long expiresAt = Long.parseLong(claims.getOrDefault("exp", "0"));
        if (expiresAt <= Instant.now().getEpochSecond()) {
            throw new JwtUnauthorizedException("登录状态已过期，请重新登录");
        }
        AuthenticatedUser user = new AuthenticatedUser(Long.valueOf(claims.get("sub")), claims.get("username"));
        return new JwtParseResult(user, expiresAt);
    }

    /**
     * 获取令牌过期秒数。
     *
     * @return 过期秒数
     */
    public long getExpiresInSeconds() {
        return jwtProperties.getExpiresInSeconds();
    }

    /**
     * 对签名原文执行 HMAC-SHA256。
     *
     * @param unsignedToken 签名原文
     * @return Base64Url 签名
     */
    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("JWT 签名失败", exception);
        }
    }

    /**
     * 解析简单 JSON payload。
     *
     * @param payload Base64Url 编码 payload
     * @return claims 映射
     */
    private Map<String, String> parsePayload(String payload) {
        String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        Map<String, String> claims = new LinkedHashMap<>();
        String content = json.substring(1, json.length() - 1);
        for (String item : content.split(",")) {
            String[] pair = item.split(":", 2);
            String key = pair[0].replace("\"", "").trim();
            String value = pair[1].replace("\"", "").trim();
            claims.put(key, value);
        }
        return claims;
    }

    /**
     * 执行 Base64Url 无填充编码。
     *
     * @param bytes 原始字节
     * @return 编码结果
     */
    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 转义 JSON 字符串。
     *
     * @param value 原始字符串
     * @return 转义后字符串
     */
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
