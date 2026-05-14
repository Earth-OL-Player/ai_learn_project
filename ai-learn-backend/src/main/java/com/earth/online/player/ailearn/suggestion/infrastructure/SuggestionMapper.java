package com.earth.online.player.ailearn.suggestion.infrastructure;

import com.earth.online.player.ailearn.suggestion.domain.Suggestion;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            INSERT INTO suggestions(user_id, title, content, type, status)
            VALUES(#{userId}, #{title}, #{content}, #{type}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Suggestion suggestion);

    /**
     * 分页查询建议列表。
     *
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 建议列表
     */
    @Select("""
            SELECT s.id, s.title, s.content, s.type, s.status, s.created_at,
                   u.id AS author_id, u.username AS author_username,
                   u.nickname AS author_nickname, u.avatar AS author_avatar
            FROM suggestions s
            INNER JOIN users u ON u.id = s.user_id AND u.deleted = 0
            WHERE s.deleted = 0
            ORDER BY s.created_at DESC, s.id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<SuggestionRecord> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计有效建议数量。
     *
     * @return 有效建议数量
     */
    @Select("SELECT COUNT(1) FROM suggestions WHERE deleted = 0")
    long countActive();
}
