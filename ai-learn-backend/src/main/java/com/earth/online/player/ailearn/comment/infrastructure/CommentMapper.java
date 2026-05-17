package com.earth.online.player.ailearn.comment.infrastructure;

import com.earth.online.player.ailearn.comment.domain.Comment;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 评论 MyBatis 仓储。
 */
@Mapper
public interface CommentMapper {

    /**
     * 新增评论。
     *
     * @param comment 评论信息
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO comments(user_id, content, parent_id, like_count)
            VALUES(#{userId}, #{content}, #{parentId}, #{likeCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Comment comment);

    /**
     * 分页查询父评论列表。
     *
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @param sort 排序方式
     * @param viewerUserId 当前查看用户ID，未登录时传0
     * @return 父评论列表
     */
    @Select("""
            <script>
            SELECT c.id, c.content, c.parent_id, c.like_count, c.created_at,
                   CASE WHEN EXISTS(
                       SELECT 1 FROM comment_likes cl
                       WHERE cl.comment_id = c.id AND cl.user_id = #{viewerUserId}
                   ) THEN 1 ELSE 0 END AS liked,
                   (
                       SELECT COUNT(1) FROM comments child
                       WHERE child.parent_id = c.id AND child.deleted = 0
                   ) AS reply_count,
                   u.id AS author_id, u.username AS author_username,
                   u.nickname AS author_nickname, u.avatar AS author_avatar,
                   u.experience AS author_experience
            FROM comments c
            INNER JOIN users u ON u.id = c.user_id AND u.deleted = 0
            WHERE c.deleted = 0 AND c.parent_id IS NULL
            ORDER BY
            <choose>
                <when test="sort == 'LATEST'">
                    c.created_at DESC, c.id DESC
                </when>
                <otherwise>
                    c.like_count DESC, c.created_at DESC, c.id DESC
                </otherwise>
            </choose>
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<CommentRecord> findParentPage(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("sort") String sort,
            @Param("viewerUserId") long viewerUserId
    );

    /**
     * 批量查询父评论下的一级子评论。
     *
     * @param parentIds 父评论ID列表
     * @param viewerUserId 当前查看用户ID，未登录时传0
     * @return 子评论列表
     */
    @Select("""
            <script>
            SELECT c.id, c.content, c.parent_id, c.like_count, c.created_at,
                   CASE WHEN EXISTS(
                       SELECT 1 FROM comment_likes cl
                       WHERE cl.comment_id = c.id AND cl.user_id = #{viewerUserId}
                   ) THEN 1 ELSE 0 END AS liked,
                   0 AS reply_count,
                   u.id AS author_id, u.username AS author_username,
                   u.nickname AS author_nickname, u.avatar AS author_avatar,
                   u.experience AS author_experience
            FROM comments c
            INNER JOIN users u ON u.id = c.user_id AND u.deleted = 0
            WHERE c.deleted = 0 AND c.parent_id IN
            <foreach collection="parentIds" item="parentId" open="(" separator="," close=")">
                #{parentId}
            </foreach>
            ORDER BY c.parent_id ASC, c.created_at ASC, c.id ASC
            </script>
            """)
    List<CommentRecord> findChildrenByParentIds(
            @Param("parentIds") List<Long> parentIds,
            @Param("viewerUserId") long viewerUserId
    );

    /**
     * 根据评论ID查询单条评论。
     *
     * @param id 评论ID
     * @param viewerUserId 当前查看用户ID，未登录时传0
     * @return 评论记录
     */
    @Select("""
            SELECT c.id, c.content, c.parent_id, c.like_count, c.created_at,
                   CASE WHEN EXISTS(
                       SELECT 1 FROM comment_likes cl
                       WHERE cl.comment_id = c.id AND cl.user_id = #{viewerUserId}
                   ) THEN 1 ELSE 0 END AS liked,
                   (
                       SELECT COUNT(1) FROM comments child
                       WHERE child.parent_id = c.id AND child.deleted = 0
                   ) AS reply_count,
                   u.id AS author_id, u.username AS author_username,
                   u.nickname AS author_nickname, u.avatar AS author_avatar,
                   u.experience AS author_experience
            FROM comments c
            INNER JOIN users u ON u.id = c.user_id AND u.deleted = 0
            WHERE c.id = #{id} AND c.deleted = 0
            """)
    CommentRecord findById(@Param("id") Long id, @Param("viewerUserId") long viewerUserId);

    /**
     * 查询评论父ID。
     *
     * @param id 评论ID
     * @return 父评论ID
     */
    @Select("SELECT parent_id FROM comments WHERE id = #{id} AND deleted = 0")
    Long findParentId(@Param("id") Long id);

    /**
     * 统计父评论数量。
     *
     * @return 父评论数量
     */
    @Select("SELECT COUNT(1) FROM comments WHERE deleted = 0 AND parent_id IS NULL")
    long countActiveParents();

    /**
     * 统计有效评论数量。
     *
     * @param id 评论ID
     * @return 有效评论数量
     */
    @Select("SELECT COUNT(1) FROM comments WHERE id = #{id} AND deleted = 0")
    int countActiveById(@Param("id") Long id);

    /**
     * 写入评论点赞明细。
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Insert("""
            INSERT IGNORE INTO comment_likes(comment_id, user_id)
            VALUES(#{commentId}, #{userId})
            """)
    int insertLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 删除评论点赞明细。
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM comment_likes WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 增加评论点赞数。
     *
     * @param id 评论ID
     * @return 影响行数
     */
    @Update("UPDATE comments SET like_count = like_count + 1 WHERE id = #{id} AND deleted = 0")
    int increaseLikeCount(@Param("id") Long id);

    /**
     * 减少评论点赞数。
     *
     * @param id 评论ID
     * @return 影响行数
     */
    @Update("UPDATE comments SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id} AND deleted = 0")
    int decreaseLikeCount(@Param("id") Long id);
}
