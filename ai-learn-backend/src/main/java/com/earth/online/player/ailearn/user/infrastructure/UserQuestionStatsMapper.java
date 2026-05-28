package com.earth.online.player.ailearn.user.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户刷题汇总 MyBatis 仓储。
 */
@Mapper
public interface UserQuestionStatsMapper {

    /**
     * 分页查询当前用户刷题汇总列表。
     *
     * @param userId 用户ID
     * @param keyword 题目关键词
     * @param questionType 题目类型
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 刷题汇总列表
     */
    @Select({
            "<script>",
            """
            SELECT stats.question_code, q.question, q.question_type, stats.answer_count,
                   stats.best_score, stats.last_score, stats.first_answered_at, stats.last_answered_at
            """,
            UserQuestionStatsSql.USER_QUESTION_STATS_FILTER_SQL,
            "ORDER BY stats.last_answered_at DESC, stats.id DESC",
            "LIMIT #{pageSize} OFFSET #{offset}",
            "</script>"
    })
    List<UserQuestionStatsRecord> findPage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("questionType") String questionType,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计当前用户刷题汇总列表数量。
     *
     * @param userId 用户ID
     * @param keyword 题目关键词
     * @param questionType 题目类型
     * @return 记录数量
     */
    @Select({
            "<script>",
            "SELECT COUNT(1)",
            UserQuestionStatsSql.USER_QUESTION_STATS_FILTER_SQL,
            "</script>"
    })
    long countPage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("questionType") String questionType
    );

    /**
     * 查询当前用户已练题型。
     *
     * @param userId 用户ID
     * @return 题型列表
     */
    @Select("""
            SELECT DISTINCT q.question_type
            FROM user_question_stats stats
            INNER JOIN questions q ON q.code = stats.question_code AND q.deleted = 0
            WHERE stats.user_id = #{userId}
              AND stats.answer_count > 0
            ORDER BY q.question_type ASC
            """)
    List<String> findQuestionTypes(@Param("userId") Long userId);

    /**
     * 查询当前用户刷题总览。
     *
     * @param userId 用户ID
     * @return 总览记录
     */
    @Select("""
            SELECT COUNT(1) AS practiced_question_count,
                   COALESCE(SUM(stats.answer_count), 0) AS total_answer_count,
                   COALESCE(AVG(stats.best_score), 0) AS average_best_score,
                   COALESCE(AVG(stats.last_score), 0) AS average_last_score,
                   SUM(CASE WHEN stats.best_score < 60 THEN 1 ELSE 0 END) AS weak_question_count,
                   MAX(stats.last_answered_at) AS last_answered_at
            FROM user_question_stats stats
            INNER JOIN questions q ON q.code = stats.question_code AND q.deleted = 0
            WHERE stats.user_id = #{userId}
              AND stats.answer_count > 0
            """)
    UserQuestionStatsOverviewRecord findOverview(@Param("userId") Long userId);

    /**
     * 按题型查询当前用户刷题汇总。
     *
     * @param userId 用户ID
     * @return 题型维度汇总列表
     */
    @Select("""
            SELECT q.question_type,
                   COUNT(1) AS question_count,
                   COALESCE(SUM(stats.answer_count), 0) AS answer_count,
                   COALESCE(AVG(stats.best_score), 0) AS average_best_score,
                   COALESCE(AVG(stats.last_score), 0) AS average_last_score,
                   SUM(CASE WHEN stats.best_score < 60 THEN 1 ELSE 0 END) AS weak_count
            FROM user_question_stats stats
            INNER JOIN questions q ON q.code = stats.question_code AND q.deleted = 0
            WHERE stats.user_id = #{userId}
              AND stats.answer_count > 0
            GROUP BY q.question_type
            ORDER BY weak_count DESC, average_best_score ASC, question_count DESC
            """)
    List<UserQuestionTypeStatsRecord> findTypeStats(@Param("userId") Long userId);
}

/**
 * 用户刷题汇总 Mapper 复用 SQL 片段。
 */
final class UserQuestionStatsSql {

    /** 当前用户已作答题目汇总通用关联和筛选条件。 */
    static final String USER_QUESTION_STATS_FILTER_SQL = """
            FROM user_question_stats stats
            INNER JOIN questions q ON q.code = stats.question_code AND q.deleted = 0
            WHERE stats.user_id = #{userId}
              AND stats.answer_count &gt; 0
            <if test='keyword != null and keyword != ""'>
              AND q.question LIKE CONCAT('%', #{keyword}, '%')
            </if>
            <if test='questionType != null and questionType != ""'>
              AND q.question_type = #{questionType}
            </if>
            """;

    /**
     * 工具类不允许实例化。
     */
    private UserQuestionStatsSql() {
    }
}
