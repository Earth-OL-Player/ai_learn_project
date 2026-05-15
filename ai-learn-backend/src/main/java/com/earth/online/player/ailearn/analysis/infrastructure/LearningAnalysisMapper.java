package com.earth.online.player.ailearn.analysis.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学习分析 MyBatis 仓储。
 */
@Mapper
public interface LearningAnalysisMapper {

    /**
     * 查询薄弱知识点。
     *
     * @param userId 用户ID
     * @return 薄弱点列表
     */
    @Select("""
            SELECT kp.id AS knowledge_point_id,
                   kp.name AS knowledge_point_name,
                   COUNT(ar.id) AS answered_count,
                   AVG(ar.score) AS average_score,
                   SUM(CASE WHEN ar.score < 60 THEN 1 ELSE 0 END) AS low_score_count,
                   MIN(q.id) AS recommended_question_id,
                   MIN(q.title) AS recommended_question_title
            FROM answer_records ar
            JOIN question_knowledge_points qkp ON qkp.question_id = ar.question_id
            JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id
            JOIN questions q ON q.id = ar.question_id
            WHERE ar.user_id = #{userId} AND kp.deleted = 0
            GROUP BY kp.id, kp.name
            ORDER BY average_score ASC, low_score_count DESC, answered_count DESC
            LIMIT 10
            """)
    List<KnowledgeWeakPointRecord> findWeakPoints(@Param("userId") Long userId);
}
