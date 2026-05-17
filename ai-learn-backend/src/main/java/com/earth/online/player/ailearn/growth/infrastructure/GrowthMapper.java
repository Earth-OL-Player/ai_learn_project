package com.earth.online.player.ailearn.growth.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 成长徽章 MyBatis 仓储。
 */
@Mapper
public interface GrowthMapper {

    /**
     * 查询用户徽章墙。
     *
     * @param userId 用户ID
     * @return 徽章列表
     */
    @Select("""
            SELECT b.id, b.name, b.description, b.icon, b.rule_code,
                   CASE WHEN ub.id IS NULL THEN 0 ELSE 1 END AS acquired,
                   ub.acquired_at
            FROM badges b
            LEFT JOIN user_badges ub ON ub.badge_id = b.id AND ub.user_id = #{userId}
            ORDER BY b.id ASC
            """)
    List<BadgeRecord> findBadgeWall(@Param("userId") Long userId);

    /**
     * 按规则查询徽章。
     *
     * @param userId 用户ID
     * @param ruleCode 规则编码
     * @return 徽章信息
     */
    @Select("""
            SELECT b.id, b.name, b.description, b.icon, b.rule_code,
                   CASE WHEN ub.id IS NULL THEN 0 ELSE 1 END AS acquired,
                   ub.acquired_at
            FROM badges b
            LEFT JOIN user_badges ub ON ub.badge_id = b.id AND ub.user_id = #{userId}
            WHERE b.rule_code = #{ruleCode}
            """)
    BadgeRecord findBadgeByRuleCode(@Param("userId") Long userId, @Param("ruleCode") String ruleCode);

    /**
     * 给用户发放徽章。
     *
     * @param userId 用户ID
     * @param ruleCode 规则编码
     * @return 影响行数
     */
    @Insert("""
            INSERT IGNORE INTO user_badges(user_id, badge_id)
            SELECT #{userId}, b.id FROM badges b WHERE b.rule_code = #{ruleCode}
            """)
    int insertUserBadge(@Param("userId") Long userId, @Param("ruleCode") String ruleCode);

    /**
     * 新增成长事件。
     *
     * @param userId 用户ID
     * @param eventType 事件类型
     * @param title 标题
     * @param description 说明
     * @param experienceDelta 经验变化
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO growth_events(user_id, event_type, title, description, experience_delta)
            VALUES(#{userId}, #{eventType}, #{title}, #{description}, #{experienceDelta})
            """)
    int insertGrowthEvent(
            @Param("userId") Long userId,
            @Param("eventType") String eventType,
            @Param("title") String title,
            @Param("description") String description,
            @Param("experienceDelta") int experienceDelta
    );

    /**
     * 查询最近成长事件。
     *
     * @param userId 用户ID
     * @param limit 数量
     * @return 事件列表
     */
    @Select("""
            SELECT id, event_type, title, description, experience_delta, created_at
            FROM growth_events
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<GrowthEventRecord> findRecentEvents(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 统计刷题评分完成次数。
     *
     * @param userId 用户ID
     * @return 完成次数
     */
    @Select("SELECT COALESCE(SUM(answer_count), 0) FROM user_question_stats WHERE user_id = #{userId}")
    long countCompletedAnswers(@Param("userId") Long userId);

    /**
     * 统计总学习天数。
     *
     * @param userId 用户ID
     * @return 学习天数
     */
    @Select("""
            SELECT COUNT(1)
            FROM (
                SELECT DATE(created_at) AS learning_day
                FROM growth_events
                WHERE user_id = #{userId} AND event_type = 'ANSWER'
                UNION
                SELECT DATE(first_answered_at) AS learning_day
                FROM user_question_stats
                WHERE user_id = #{userId} AND first_answered_at IS NOT NULL
                UNION
                SELECT DATE(last_answered_at) AS learning_day
                FROM user_question_stats
                WHERE user_id = #{userId} AND last_answered_at IS NOT NULL
            ) learning_days
            """)
    int countLearningDays(@Param("userId") Long userId);

    /**
     * 查询最高分平均值。
     *
     * @param userId 用户ID
     * @return 平均最高分
     */
    @Select("SELECT COALESCE(AVG(best_score), 0) FROM user_question_stats WHERE user_id = #{userId} AND answer_count > 0")
    double averageBestScore(@Param("userId") Long userId);

    /**
     * 查询最高分总和。
     *
     * @param userId 用户ID
     * @return 总经验值
     */
    @Select("SELECT COALESCE(SUM(best_score), 0) FROM user_question_stats WHERE user_id = #{userId}")
    int sumBestScores(@Param("userId") Long userId);
}
