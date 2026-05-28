package com.earth.online.player.ailearn.common.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * JWT 服务端失效服务。
 */
@Service
public class TokenInvalidationService {

    private final InvalidatedTokenMapper invalidatedTokenMapper;

    /**
     * 创建 JWT 服务端失效服务。
     *
     * @param invalidatedTokenMapper 失效令牌仓储
     */
    public TokenInvalidationService(InvalidatedTokenMapper invalidatedTokenMapper) {
        this.invalidatedTokenMapper = invalidatedTokenMapper;
    }

    /**
     * 将当前令牌标记为服务端失效。
     *
     * @param parseResult JWT 解析结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void invalidate(JwtParseResult parseResult) {
        if (!StringUtils.hasText(parseResult.tokenId())) {
            throw new JwtUnauthorizedException(AuthMessages.SESSION_INVALID_MESSAGE);
        }

        // 顺手清理自然过期记录，控制失效表体量。
        LocalDateTime now = LocalDateTime.now();
        invalidatedTokenMapper.deleteExpired(now);
        invalidatedTokenMapper.insert(
                parseResult.tokenId(),
                parseResult.user().userId(),
                toLocalDateTime(parseResult.expiresAt())
        );
    }

    /**
     * 校验令牌是否仍处于服务端有效状态。
     *
     * @param tokenId JWT 唯一标识
     */
    public void ensureTokenActive(String tokenId) {
        if (!StringUtils.hasText(tokenId) || invalidatedTokenMapper.countByTokenId(tokenId) > 0) {
            throw new JwtUnauthorizedException(AuthMessages.SESSION_INVALID_MESSAGE);
        }
    }

    /**
     * 将时间戳秒数转换为本地数据库时间。
     *
     * @param epochSeconds 时间戳秒数
     * @return 本地时间
     */
    private LocalDateTime toLocalDateTime(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }
}
