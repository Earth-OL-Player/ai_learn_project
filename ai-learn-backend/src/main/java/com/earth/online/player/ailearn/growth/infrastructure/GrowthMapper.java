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
     * 查询全部徽章静态定义。
     *
     * @return 徽章列表
     */
    @Select("""
            SELECT b.id, b.name, b.description, b.icon, b.rule_code,
                   0 AS acquired, NULL AS acquired_at
            FROM badges b
            ORDER BY b.id ASC
            """)
    List<BadgeRecord> findAllBadges();

    /**
     * 查询用户已获得徽章。
     *
     * @param userId 用户ID
     * @return 已获得徽章列表
     */
    @Select("""
            SELECT b.id, b.name, b.description, b.icon, b.rule_code,
                   1 AS acquired, ub.acquired_at
            FROM user_badges ub
            INNER JOIN badges b ON b.id = ub.badge_id
            WHERE ub.user_id = #{userId}
            ORDER BY b.id ASC
            """)
    List<BadgeRecord> findAcquiredBadges(@Param("userId") Long userId);

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
     * 统计已完成刷题题目数。
     *
     * @param userId 用户ID
     * @return 完成题目数
     */
    @Select("SELECT COUNT(1) FROM user_question_stats WHERE user_id = #{userId} AND answer_count > 0")
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

}

