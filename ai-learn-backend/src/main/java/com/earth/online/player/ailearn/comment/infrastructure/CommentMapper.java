package com.earth.online.player.ailearn.comment.infrastructure;

import com.earth.online.player.ailearn.comment.domain.Comment;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
     * 分页查询评论列表。
     *
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 评论列表
     */
    @Select("""
            SELECT c.id, c.content, c.parent_id, c.like_count, c.created_at,
                   u.id AS author_id, u.username AS author_username,
                   u.nickname AS author_nickname, u.avatar AS author_avatar
            FROM comments c
            INNER JOIN users u ON u.id = c.user_id AND u.deleted = 0
            WHERE c.deleted = 0
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<CommentRecord> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计有效评论数量。
     *
     * @return 有效评论数量
     */
    @Select("SELECT COUNT(1) FROM comments WHERE deleted = 0")
    long countActive();
}
