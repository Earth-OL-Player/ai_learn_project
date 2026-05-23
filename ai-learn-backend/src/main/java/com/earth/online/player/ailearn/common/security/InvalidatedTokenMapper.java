package com.earth.online.player.ailearn.common.security;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 失效令牌 MyBatis 仓储。
 */
@Mapper
public interface InvalidatedTokenMapper {

    /**
     * 新增已失效令牌记录。
     *
     * @param tokenId JWT 唯一标识
     * @param userId 用户ID
     * @param expiresAt 令牌过期时间
     * @return 影响行数
     */
    @Insert("""
            INSERT IGNORE INTO invalidated_tokens(token_id, user_id, expires_at)
            VALUES(#{tokenId}, #{userId}, #{expiresAt})
            """)
    int insert(
            @Param("tokenId") String tokenId,
            @Param("userId") Long userId,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    /**
     * 判断令牌是否已经服务端失效。
     *
     * @param tokenId JWT 唯一标识
     * @return 命中数量
     */
    @Select("SELECT COUNT(1) FROM invalidated_tokens WHERE token_id = #{tokenId}")
    int countByTokenId(@Param("tokenId") String tokenId);

    /**
     * 清理已经自然过期的失效令牌记录。
     *
     * @param now 当前时间
     * @return 删除行数
     */
    @Delete("DELETE FROM invalidated_tokens WHERE expires_at <= #{now}")
    int deleteExpired(@Param("now") LocalDateTime now);
}
