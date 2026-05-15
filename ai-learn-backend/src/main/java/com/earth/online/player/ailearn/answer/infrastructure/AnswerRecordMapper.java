package com.earth.online.player.ailearn.answer.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 答题记录 MyBatis 仓储。
 */
@Mapper
public interface AnswerRecordMapper {

    /**
     * 新增答题记录。
     *
     * @param record 答题记录
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO answer_records(user_id, question_id, session_id, user_answer, score, is_correct,
                                       ai_feedback, grading_source, improvement_advice, duration_seconds, first_attempt)
            VALUES(#{userId}, #{questionId}, #{sessionId}, #{userAnswer}, #{score}, #{correct},
                   #{aiFeedback}, #{gradingSource}, #{improvementAdvice}, #{durationSeconds}, #{firstAttempt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AnswerRecordEntity record);

    /**
     * 统计用户指定题目的历史答题数。
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 答题数量
     */
    @Select("""
            SELECT COUNT(1)
            FROM answer_records
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    long countByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 分页查询当前用户答题记录。
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 答题记录列表
     */
    @Select("""
            SELECT ar.id, ar.question_id, q.title AS question_title, q.question_type, q.difficulty,
                   ar.score, ar.is_correct AS correct, ar.improvement_advice, ar.duration_seconds,
                   ar.first_attempt, ar.created_at
            FROM answer_records ar
            JOIN questions q ON q.id = ar.question_id
            WHERE ar.user_id = #{userId}
            ORDER BY ar.created_at DESC, ar.id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<AnswerRecordItemRecord> findPageByUser(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计当前用户答题记录数量。
     *
     * @param userId 用户ID
     * @return 数量
     */
    @Select("SELECT COUNT(1) FROM answer_records WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Long userId);

    /**
     * 查询当前用户平均得分。
     *
     * @param userId 用户ID
     * @return 平均得分
     */
    @Select("SELECT COALESCE(AVG(score), 0) FROM answer_records WHERE user_id = #{userId}")
    double averageScoreByUser(@Param("userId") Long userId);

    /**
     * 查询用户某题历史最低分。
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 历史最低分
     */
    @Select("""
            SELECT MIN(score)
            FROM answer_records
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    Integer minScoreByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
