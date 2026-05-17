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
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 题目列表
     */
    @Select("""
            <script>
            SELECT q.id, q.code, q.question, q.question_type, q.created_at,
                   q.importance_score, q.occurrence_count
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
            ORDER BY q.created_at DESC, q.id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<QuestionListRecord> findPage(
            @Param("keyword") String keyword,
            @Param("questionType") String questionType,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计系统题库数量。
     *
     * @param keyword 关键词
     * @param questionType 题目分类
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
            </script>
            """)
    long countPage(
            @Param("keyword") String keyword,
            @Param("questionType") String questionType
    );

    /**
     * 查询系统题目详情。
     *
     * @param id 题目ID
     * @return 题目详情
     */
    @Select("""
            SELECT q.id, q.code, q.question, q.question_type, q.standard_answer,
                   q.importance_score, q.occurrence_count, q.created_at, q.updated_at
            FROM questions q
            WHERE q.id = #{id} AND q.deleted = 0
            """)
    QuestionDetailRecord findDetailById(@Param("id") Long id);

    /**
     * 查询热门面经阅读文档所需题目。
     *
     * @param questionType 题目分类
     * @return 热门面经题目详情列表
     */
    @Select("""
            <script>
            SELECT q.id, q.code, q.question, q.question_type, q.standard_answer,
                   q.importance_score, q.occurrence_count, q.created_at, q.updated_at
            FROM questions q
            WHERE q.deleted = 0
            <if test='questionType != null and questionType != ""'>
              AND q.question_type = #{questionType}
            </if>
            ORDER BY q.question_type ASC,
                     q.importance_score DESC,
                     q.occurrence_count DESC,
                     q.created_at DESC,
                     q.id DESC
            </script>
            """)
    List<QuestionDetailRecord> findInterviewDocument(@Param("questionType") String questionType);

    /**
     * 查询题目表中实际存在的分类。
     *
     * @return 题目分类列表
     */
    @Select("""
            SELECT DISTINCT question_type
            FROM questions
            WHERE deleted = 0
            ORDER BY question_type ASC
            """)
    List<String> findQuestionTypes();
}
