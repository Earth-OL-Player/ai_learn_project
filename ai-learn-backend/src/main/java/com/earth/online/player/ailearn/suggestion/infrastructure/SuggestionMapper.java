package com.earth.online.player.ailearn.suggestion.infrastructure;

import com.earth.online.player.ailearn.suggestion.domain.Suggestion;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 建议 MyBatis 仓储。
 */
@Mapper
public interface SuggestionMapper {

    /**
     * 新增建议。
     *
     * @param suggestion 建议信息
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO suggestions(user_id, content, type, like_count)
            VALUES(#{userId}, #{content}, #{type}, #{likeCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Suggestion suggestion);

    /**
     * 分页查询建议列表。
     *
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @param sort 排序方式
     * @param viewerUserId 当前查看用户ID，未登录时传0
     * @return 建议列表
     */
    @Select("""
            <script>
            SELECT s.id, s.content, s.type, s.like_count, s.created_at,
                   CASE WHEN EXISTS(
                       SELECT 1 FROM suggestion_likes sl
                       WHERE sl.suggestion_id = s.id AND sl.user_id = #{viewerUserId}
                   ) THEN 1 ELSE 0 END AS liked,
                   u.id AS author_id, u.username AS author_username,
                   u.nickname AS author_nickname, u.avatar AS author_avatar,
                   u.experience AS author_experience
            FROM suggestions s
            INNER JOIN users u ON u.id = s.user_id AND u.deleted = 0
            WHERE s.deleted = 0
            ORDER BY
            <choose>
                <when test="sort == 'LATEST'">
                    s.created_at DESC, s.id DESC
                </when>
                <otherwise>
                    s.like_count DESC, s.created_at DESC, s.id DESC
                </otherwise>
            </choose>
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<SuggestionRecord> findPage(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("sort") String sort,
            @Param("viewerUserId") long viewerUserId
    );

    /**
     * 根据建议ID查询单条建议。
     *
     * @param id 建议ID
     * @param viewerUserId 当前查看用户ID，未登录时传0
     * @return 建议记录
     */
    @Select("""
            SELECT s.id, s.content, s.type, s.like_count, s.created_at,
                   CASE WHEN EXISTS(
                       SELECT 1 FROM suggestion_likes sl
                       WHERE sl.suggestion_id = s.id AND sl.user_id = #{viewerUserId}
                   ) THEN 1 ELSE 0 END AS liked,
                   u.id AS author_id, u.username AS author_username,
                   u.nickname AS author_nickname, u.avatar AS author_avatar,
                   u.experience AS author_experience
            FROM suggestions s
            INNER JOIN users u ON u.id = s.user_id AND u.deleted = 0
            WHERE s.id = #{id} AND s.deleted = 0
            """)
    SuggestionRecord findById(@Param("id") Long id, @Param("viewerUserId") long viewerUserId);

    /**
     * 统计有效建议数量。
     *
     * @return 有效建议数量
     */
    @Select("SELECT COUNT(1) FROM suggestions WHERE deleted = 0")
    long countActive();

    /**
     * 统计有效建议数量。
     *
     * @param id 建议ID
     * @return 有效建议数量
     */
    @Select("SELECT COUNT(1) FROM suggestions WHERE id = #{id} AND deleted = 0")
    int countActiveById(@Param("id") Long id);

    /**
     * 写入建议点赞明细。
     *
     * @param suggestionId 建议ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Insert("""
            INSERT IGNORE INTO suggestion_likes(suggestion_id, user_id)
            VALUES(#{suggestionId}, #{userId})
            """)
    int insertLike(@Param("suggestionId") Long suggestionId, @Param("userId") Long userId);

    /**
     * 删除建议点赞明细。
     *
     * @param suggestionId 建议ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM suggestion_likes WHERE suggestion_id = #{suggestionId} AND user_id = #{userId}")
    int deleteLike(@Param("suggestionId") Long suggestionId, @Param("userId") Long userId);

    /**
     * 增加建议点赞数。
     *
     * @param id 建议ID
     * @return 影响行数
     */
    @Update("UPDATE suggestions SET like_count = like_count + 1 WHERE id = #{id} AND deleted = 0")
    int increaseLikeCount(@Param("id") Long id);

    /**
     * 减少建议点赞数。
     *
     * @param id 建议ID
     * @return 影响行数
     */
    @Update("UPDATE suggestions SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id} AND deleted = 0")
    int decreaseLikeCount(@Param("id") Long id);
}
