package com.earth.online.player.ailearn.question.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 题库 MyBatis 仓储。
 */
@Mapper
public interface QuestionMapper {

    /**
     * 分页查询系统题库。
     *
     * @param keyword 关键词
     * @param questionType 题目分类
     * @param knowledgePointId 知识点ID
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 题目列表
     */
    @Select("""
            <script>
            SELECT q.id, q.code, q.question, q.question_type, q.created_at,
                   q.importance_score, q.occurrence_count,
                   GROUP_CONCAT(kp.name ORDER BY kp.id SEPARATOR ',') AS knowledge_point_names
            FROM questions q
            LEFT JOIN question_knowledge_points qkp ON qkp.question_id = q.id
            LEFT JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id AND kp.deleted = 0
            WHERE q.deleted = 0
            <if test='keyword != null and keyword != ""'>
              AND (q.code LIKE CONCAT('%', #{keyword}, '%')
                   OR q.question LIKE CONCAT('%', #{keyword}, '%')
                   OR q.standard_answer LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='questionType != null and questionType != ""'>
              AND q.question_type = #{questionType}
            </if>
            <if test='knowledgePointId != null'>
              AND EXISTS (
                  SELECT 1 FROM question_knowledge_points filter_qkp
                  WHERE filter_qkp.question_id = q.id AND filter_qkp.knowledge_point_id = #{knowledgePointId}
              )
            </if>
            GROUP BY q.id, q.code, q.question, q.question_type, q.created_at,
                     q.importance_score, q.occurrence_count
            ORDER BY q.created_at DESC, q.id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<QuestionListRecord> findPage(
            @Param("keyword") String keyword,
            @Param("questionType") String questionType,
            @Param("knowledgePointId") Long knowledgePointId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计系统题库数量。
     *
     * @param keyword 关键词
     * @param questionType 题目分类
     * @param knowledgePointId 知识点ID
     * @return 题目数量
     */
    @Select("""
            <script>
            SELECT COUNT(1)
            FROM questions q
            WHERE q.deleted = 0
            <if test='keyword != null and keyword != ""'>
              AND (q.code LIKE CONCAT('%', #{keyword}, '%')
                   OR q.question LIKE CONCAT('%', #{keyword}, '%')
                   OR q.standard_answer LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='questionType != null and questionType != ""'>
              AND q.question_type = #{questionType}
            </if>
            <if test='knowledgePointId != null'>
              AND EXISTS (
                  SELECT 1 FROM question_knowledge_points filter_qkp
                  WHERE filter_qkp.question_id = q.id AND filter_qkp.knowledge_point_id = #{knowledgePointId}
              )
            </if>
            </script>
            """)
    long countPage(
            @Param("keyword") String keyword,
            @Param("questionType") String questionType,
            @Param("knowledgePointId") Long knowledgePointId
    );

    /**
     * 查询系统题目详情。
     *
     * @param id 题目ID
     * @return 题目详情
     */
    @Select("""
            SELECT q.id, q.code, q.question, q.question_type, q.standard_answer,
                   q.importance_score, q.occurrence_count, q.created_at, q.updated_at,
                   GROUP_CONCAT(kp.name ORDER BY kp.id SEPARATOR ',') AS knowledge_point_names
            FROM questions q
            LEFT JOIN question_knowledge_points qkp ON qkp.question_id = q.id
            LEFT JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id AND kp.deleted = 0
            WHERE q.id = #{id} AND q.deleted = 0
            GROUP BY q.id, q.code, q.question, q.question_type, q.standard_answer,
                     q.importance_score, q.occurrence_count, q.created_at, q.updated_at
            """)
    QuestionDetailRecord findDetailById(@Param("id") Long id);
}
