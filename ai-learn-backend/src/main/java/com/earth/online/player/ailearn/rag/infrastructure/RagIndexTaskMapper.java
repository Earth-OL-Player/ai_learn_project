package com.earth.online.player.ailearn.rag.infrastructure;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * RAG任务 MyBatis 仓储。
 */
@Mapper
public interface RagIndexTaskMapper {

    /** 新增任务摘要。 */
    @Insert("""
            INSERT INTO rag_index_tasks(task_id, user_id, source_type, status, message)
            VALUES(#{taskId}, #{userId}, #{sourceType}, #{status}, #{message})
            """)
    int insertTask(
            @Param("taskId") String taskId,
            @Param("userId") Long userId,
            @Param("sourceType") String sourceType,
            @Param("status") String status,
            @Param("message") String message
    );

    /** 更新任务状态。 */
    @Update("""
            UPDATE rag_index_tasks
            SET status = #{status}, message = #{message}
            WHERE task_id = #{taskId} AND user_id = #{userId}
            """)
    int updateTask(
            @Param("taskId") String taskId,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("message") String message
    );

    /** 查询任务。 */
    @Select("""
            SELECT task_id, source_type, status, message, created_at, updated_at
            FROM rag_index_tasks
            WHERE task_id = #{taskId} AND user_id = #{userId}
            """)
    RagIndexTaskRecord findMine(@Param("taskId") String taskId, @Param("userId") Long userId);
}
