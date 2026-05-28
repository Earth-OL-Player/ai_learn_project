package com.earth.online.player.ailearn.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT 令牌服务。
 */
@Component
public class JwtTokenService {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String USERNAME_CLAIM = "username";
    private static final String PLACEHOLDER_SECRET_KEYWORD = "占位符";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    /**
     * 创建 JWT 令牌服务。
     *
     * @param jwtProperties JWT 配置
     */
    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        validateSecret(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌。
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT 令牌
     */
    public String generateToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getExpiresInSeconds());

        // 使用成熟 JWT 库统一生成标准 claims 和 HS256 签名。
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(USERNAME_CLAIM, username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
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
            throw new JwtUnauthorizedException(AuthMessages.SESSION_INVALID_MESSAGE);
        }

        try {
            // 解析过程由 JJWT 完成签名、格式和过期时间校验。
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            AuthenticatedUser user = buildAuthenticatedUser(claims);
            return new JwtParseResult(user, claims.getId(), claims.getExpiration().toInstant().getEpochSecond());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtUnauthorizedException(AuthMessages.SESSION_EXPIRED_MESSAGE);
        }
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
     * 根据 JWT claims 构造认证用户。
     *
     * @param claims JWT claims
     * @return 认证用户
     */
    private AuthenticatedUser buildAuthenticatedUser(Claims claims) {
        String subject = claims.getSubject();
        String username = claims.get(USERNAME_CLAIM, String.class);
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(username)) {
            throw new JwtUnauthorizedException(AuthMessages.SESSION_INVALID_MESSAGE);
        }
        return new AuthenticatedUser(Long.valueOf(subject), username);
    }

    /**
     * 校验 JWT 密钥强度。
     *
     * @param secret JWT 密钥
     */
    private void validateSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET不能为空");
        }

        // HS256 至少需要 256 bit 密钥，且禁止使用仓库中的占位值启动服务。
        boolean weakSecret = secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES
                || secret.contains(PLACEHOLDER_SECRET_KEYWORD);

        if (weakSecret) {
            throw new IllegalStateException("JWT_SECRET必须是至少32字节的高强度随机密钥，且不能使用占位符");
        }
    }
}
