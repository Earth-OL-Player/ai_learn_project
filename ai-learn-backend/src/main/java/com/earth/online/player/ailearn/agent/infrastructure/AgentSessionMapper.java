package com.earth.online.player.ailearn.agent.infrastructure;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 刷题会话 MyBatis 仓储。
 */
@Mapper
public interface AgentSessionMapper {

    /**
     * 新增刷题会话。
     *
     * @param record 会话记录
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO agent_sessions(user_id, question_id, status)
            VALUES(#{userId}, #{questionId}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AgentSessionRecord record);

    /**
     * 查询刷题会话。
     *
     * @param id 会话ID
     * @return 会话记录
     */
    @Select("""
            SELECT id, user_id, question_id, status, started_at, submitted_at
            FROM agent_sessions
            WHERE id = #{id}
            """)
    AgentSessionRecord findById(@Param("id") Long id);

    /**
     * 标记会话已提交。
     *
     * @param id 会话ID
     * @return 影响行数
     */
    @Update("""
            UPDATE agent_sessions
            SET status = 'SUBMITTED', submitted_at = NOW()
            WHERE id = #{id} AND status = 'STARTED'
            """)
    int markSubmitted(@Param("id") Long id);
}
