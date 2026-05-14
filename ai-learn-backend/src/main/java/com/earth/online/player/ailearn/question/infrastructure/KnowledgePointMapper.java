package com.earth.online.player.ailearn.question.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 知识点 MyBatis 仓储。
 */
@Mapper
public interface KnowledgePointMapper {

    /**
     * 查询有效知识点列表。
     *
     * @return 知识点列表
     */
    @Select("""
            SELECT id, name, description
            FROM knowledge_points
            WHERE deleted = 0
            ORDER BY id ASC
            """)
    List<KnowledgePointRecord> findAllActive();
}
