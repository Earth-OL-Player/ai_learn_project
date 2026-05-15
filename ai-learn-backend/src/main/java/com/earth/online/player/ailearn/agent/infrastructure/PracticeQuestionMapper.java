package com.earth.online.player.ailearn.agent.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 刷题题目 MyBatis 查询仓储。
 */
@Mapper
public interface PracticeQuestionMapper {

    /**
     * 按本期推荐规则选择一道默认题。
     *
     * @param userId 用户ID
     * @param difficulty 难度
     * @param questionType 题型
     * @param knowledgePointIds 知识点ID列表
     * @param sourceScope 题库范围
     * @return 推荐题目
     */
    @Select("""
            <script>
            SELECT q.id, q.title, q.content, q.question_type, q.difficulty, q.standard_answer,
                   q.source_type, q.owner_user_id,
                   GROUP_CONCAT(kp.name ORDER BY kp.id SEPARATOR ',') AS knowledge_point_names
            FROM questions q
            LEFT JOIN question_knowledge_points qkp ON qkp.question_id = q.id
            LEFT JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id AND kp.deleted = 0
            WHERE q.deleted = 0
            <choose>
              <when test='sourceScope == "MINE"'>
                AND q.owner_user_id = #{userId} AND q.source_type = 'USER_UPLOAD'
              </when>
              <when test='sourceScope == "MIXED"'>
                AND ((q.owner_user_id IS NULL AND q.source_type = 'DEFAULT')
                     OR (q.owner_user_id = #{userId} AND q.source_type = 'USER_UPLOAD'))
              </when>
              <otherwise>
                AND q.owner_user_id IS NULL AND q.source_type = 'DEFAULT'
              </otherwise>
            </choose>
            <if test='difficulty != null and difficulty != ""'>
              AND q.difficulty = #{difficulty}
            </if>
            <if test='questionType != null and questionType != ""'>
              AND q.question_type = #{questionType}
            </if>
            <if test='knowledgePointIds != null and knowledgePointIds.size() > 0'>
              AND EXISTS (
                  SELECT 1 FROM question_knowledge_points filter_qkp
                  WHERE filter_qkp.question_id = q.id
                    AND filter_qkp.knowledge_point_id IN
                    <foreach collection='knowledgePointIds' item='knowledgePointId' open='(' separator=',' close=')'>
                      #{knowledgePointId}
                    </foreach>
              )
            </if>
            GROUP BY q.id, q.title, q.content, q.question_type, q.difficulty, q.standard_answer, q.source_type, q.owner_user_id
            ORDER BY (
                     10
                     + CASE WHEN NOT EXISTS (
                         SELECT 1 FROM answer_records ar WHERE ar.user_id = #{userId} AND ar.question_id = q.id
                       ) THEN 30 ELSE 0 END
                     + CASE WHEN COALESCE((SELECT MIN(ar2.score) FROM answer_records ar2
                                           WHERE ar2.user_id = #{userId} AND ar2.question_id = q.id), 100) &lt; 60
                       THEN 25 ELSE 0 END
                     + CASE WHEN q.owner_user_id = #{userId} THEN 10 ELSE 0 END
                     - CASE WHEN EXISTS (
                         SELECT 1 FROM answer_records ar3
                         WHERE ar3.user_id = #{userId} AND ar3.question_id = q.id
                           AND ar3.score &gt;= 85 AND ar3.created_at &gt;= DATE_SUB(NOW(), INTERVAL 7 DAY)
                       ) THEN 20 ELSE 0 END
                     ) DESC,
                     q.id ASC
            LIMIT 1
            </script>
            """)
    PracticeQuestionRecord findRecommended(
            @Param("userId") Long userId,
            @Param("difficulty") String difficulty,
            @Param("questionType") String questionType,
            @Param("knowledgePointIds") List<Long> knowledgePointIds,
            @Param("sourceScope") String sourceScope
    );

    /**
     * 查询刷题题目详情。
     *
     * @param id 题目ID
     * @param userId 用户ID
     * @return 题目详情
     */
    @Select("""
            SELECT q.id, q.title, q.content, q.question_type, q.difficulty, q.standard_answer,
                   q.source_type, q.owner_user_id,
                   GROUP_CONCAT(kp.name ORDER BY kp.id SEPARATOR ',') AS knowledge_point_names
            FROM questions q
            LEFT JOIN question_knowledge_points qkp ON qkp.question_id = q.id
            LEFT JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id AND kp.deleted = 0
            WHERE q.id = #{id} AND q.deleted = 0
              AND ((q.owner_user_id IS NULL AND q.source_type = 'DEFAULT')
                   OR (q.owner_user_id = #{userId} AND q.source_type = 'USER_UPLOAD'))
            GROUP BY q.id, q.title, q.content, q.question_type, q.difficulty, q.standard_answer, q.source_type, q.owner_user_id
            """)
    PracticeQuestionRecord findById(@Param("id") Long id, @Param("userId") Long userId);
}
