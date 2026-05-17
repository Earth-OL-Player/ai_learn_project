package com.earth.online.player.ailearn.practice.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * AI 智能刷题 MyBatis 仓储。
 */
@Mapper
public interface PracticeMapper {

    /**
     * 查询可用题目分类。
     *
     * @return 分类列表
     */
    @Select("""
            SELECT DISTINCT question_type
            FROM questions
            WHERE deleted = 0
            ORDER BY question_type ASC
            """)
    List<String> findQuestionTypes();

    /**
     * 查询候选题目。
     *
     * @param userId 用户ID
     * @param questionTypes 题目分类
     * @return 候选题目
     */
    @Select("""
            <script>
            SELECT q.id, q.code, q.question, q.question_type, q.standard_answer,
                   q.importance_score, q.occurrence_count,
                   COALESCE(stats.answer_count, 0) AS answered_count,
                   COALESCE(stats.best_score, 0) AS best_score
            FROM questions q
            LEFT JOIN user_question_stats stats
              ON stats.question_code = q.code AND stats.user_id = #{userId}
            WHERE q.deleted = 0
            <if test='questionTypes != null and questionTypes.size() > 0'>
              AND q.question_type IN
              <foreach collection='questionTypes' item='item' open='(' separator=',' close=')'>
                #{item}
              </foreach>
            </if>
            ORDER BY q.importance_score DESC, q.occurrence_count DESC, q.id DESC
            LIMIT 500
            </script>
            """)
    List<PracticeQuestionRecord> findCandidates(
            @Param("userId") Long userId,
            @Param("questionTypes") List<String> questionTypes
    );

    /**
     * 按题目编码查询题目。
     *
     * @param userId 用户ID
     * @param questionCode 题目编码
     * @return 题目记录
     */
    @Select("""
            SELECT q.id, q.code, q.question, q.question_type, q.standard_answer,
                   q.importance_score, q.occurrence_count,
                   COALESCE(stats.answer_count, 0) AS answered_count,
                   COALESCE(stats.best_score, 0) AS best_score
            FROM questions q
            LEFT JOIN user_question_stats stats
              ON stats.question_code = q.code AND stats.user_id = #{userId}
            WHERE q.code = #{questionCode} AND q.deleted = 0
            """)
    PracticeQuestionRecord findQuestionByCode(
            @Param("userId") Long userId,
            @Param("questionCode") String questionCode
    );

    /**
     * 查询当前刷题状态。
     *
     * @param userId 用户ID
     * @return 刷题状态
     */
    @Select("""
            SELECT id, user_id, question_code, phase, last_score, last_answer_text
            FROM user_practice_sessions
            WHERE user_id = #{userId}
            """)
    PracticeSessionRecord findSession(@Param("userId") Long userId);

    /**
     * 保存当前出题状态。
     *
     * @param userId 用户ID
     * @param questionCode 题目编码
     * @param phase 阶段
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO user_practice_sessions(user_id, question_code, phase, last_score, last_answer_text, started_at, answered_at)
            VALUES(#{userId}, #{questionCode}, #{phase}, NULL, NULL, NOW(), NULL)
            ON DUPLICATE KEY UPDATE question_code = VALUES(question_code),
                                    phase = VALUES(phase),
                                    last_score = NULL,
                                    last_answer_text = NULL,
                                    started_at = NOW(),
                                    answered_at = NULL
            """)
    int upsertQuestionSession(
            @Param("userId") Long userId,
            @Param("questionCode") String questionCode,
            @Param("phase") String phase
    );

    /**
     * 更新当前刷题阶段。
     *
     * @param userId 用户ID
     * @param phase 阶段
     * @param lastScore 最近得分
     * @param lastAnswerText 最近一次答案原文
     * @return 影响行数
     */
    @Update("""
            UPDATE user_practice_sessions
            SET phase = #{phase}, last_score = #{lastScore}, last_answer_text = #{lastAnswerText}, answered_at = NOW()
            WHERE user_id = #{userId}
            """)
    int updateSessionPhase(
            @Param("userId") Long userId,
            @Param("phase") String phase,
            @Param("lastScore") Integer lastScore,
            @Param("lastAnswerText") String lastAnswerText
    );

    /**
     * 查询用户题目汇总。
     *
     * @param userId 用户ID
     * @param questionCode 题目编码
     * @return 汇总记录
     */
    @Select("""
            SELECT id, user_id, question_code, answer_count, best_score, last_score
            FROM user_question_stats
            WHERE user_id = #{userId} AND question_code = #{questionCode}
            """)
    PracticeStatRecord findStat(
            @Param("userId") Long userId,
            @Param("questionCode") String questionCode
    );

    /**
     * 新增或更新用户题目汇总。
     *
     * @param userId 用户ID
     * @param questionCode 题目编码
     * @param score 本次得分
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO user_question_stats(user_id, question_code, answer_count, best_score, last_score,
                                            first_answered_at, last_answered_at)
            VALUES(#{userId}, #{questionCode}, 1, #{score}, #{score}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE answer_count = answer_count + 1,
                                    best_score = GREATEST(best_score, VALUES(best_score)),
                                    last_score = VALUES(last_score),
                                    last_answered_at = NOW()
            """)
    int upsertStat(
            @Param("userId") Long userId,
            @Param("questionCode") String questionCode,
            @Param("score") int score
    );
}
