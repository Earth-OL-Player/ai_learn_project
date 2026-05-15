package com.earth.online.player.ailearn.question.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 个人题库 MyBatis 仓储。
 */
@Mapper
public interface MyQuestionMapper {

    /**
     * 分页查询我的题库。
     *
     * @param userId 用户ID
     * @param keyword 关键词
     * @param difficulty 难度
     * @param questionType 题型
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 题目列表
     */
    @Select("""
            <script>
            SELECT q.id, q.title, q.content, q.question_type, q.difficulty, q.tags,
                   q.standard_answer, q.analysis, q.created_at, q.updated_at,
                   GROUP_CONCAT(kp.name ORDER BY kp.id SEPARATOR ',') AS knowledge_point_names
            FROM questions q
            LEFT JOIN question_knowledge_points qkp ON qkp.question_id = q.id
            LEFT JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id AND kp.deleted = 0
            WHERE q.deleted = 0 AND q.owner_user_id = #{userId} AND q.source_type = 'USER_UPLOAD'
            <if test='keyword != null and keyword != ""'>
              AND (q.title LIKE CONCAT('%', #{keyword}, '%') OR q.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='difficulty != null and difficulty != ""'>
              AND q.difficulty = #{difficulty}
            </if>
            <if test='questionType != null and questionType != ""'>
              AND q.question_type = #{questionType}
            </if>
            GROUP BY q.id, q.title, q.content, q.question_type, q.difficulty, q.tags,
                     q.standard_answer, q.analysis, q.created_at, q.updated_at
            ORDER BY q.created_at DESC, q.id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<MyQuestionRecord> findPage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("difficulty") String difficulty,
            @Param("questionType") String questionType,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计我的题库数量。
     *
     * @param userId 用户ID
     * @param keyword 关键词
     * @param difficulty 难度
     * @param questionType 题型
     * @return 总数
     */
    @Select("""
            <script>
            SELECT COUNT(1)
            FROM questions q
            WHERE q.deleted = 0 AND q.owner_user_id = #{userId} AND q.source_type = 'USER_UPLOAD'
            <if test='keyword != null and keyword != ""'>
              AND (q.title LIKE CONCAT('%', #{keyword}, '%') OR q.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='difficulty != null and difficulty != ""'>
              AND q.difficulty = #{difficulty}
            </if>
            <if test='questionType != null and questionType != ""'>
              AND q.question_type = #{questionType}
            </if>
            </script>
            """)
    long countPage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("difficulty") String difficulty,
            @Param("questionType") String questionType
    );

    /**
     * 新增题目。
     *
     * @param record 新增记录
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO questions(owner_user_id, title, content, question_type, difficulty, tags,
                                  standard_answer, analysis, source_type)
            VALUES(#{ownerUserId}, #{title}, #{content}, #{questionType}, #{difficulty}, #{tags},
                   #{standardAnswer}, #{analysis}, #{sourceType})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertQuestion(QuestionInsertRecord record);

    /**
     * 按名称查询知识点ID。
     *
     * @param name 知识点名称
     * @return 知识点ID
     */
    @Select("SELECT id FROM knowledge_points WHERE name = #{name} AND deleted = 0 LIMIT 1")
    Long findKnowledgePointIdByName(@Param("name") String name);

    /**
     * 新增知识点。
     *
     * @param name 名称
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO knowledge_points(name, description)
            VALUES(#{name}, #{description})
            ON DUPLICATE KEY UPDATE deleted = 0, description = COALESCE(description, VALUES(description))
            """)
    int insertKnowledgePoint(@Param("name") String name, @Param("description") String description);

    /**
     * 绑定题目知识点。
     *
     * @param questionId 题目ID
     * @param knowledgePointId 知识点ID
     * @return 影响行数
     */
    @Insert("""
            INSERT IGNORE INTO question_knowledge_points(question_id, knowledge_point_id)
            VALUES(#{questionId}, #{knowledgePointId})
            """)
    int insertQuestionKnowledgePoint(
            @Param("questionId") Long questionId,
            @Param("knowledgePointId") Long knowledgePointId
    );

    /**
     * 逻辑删除当前用户个人题目。
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 影响行数
     */
    @Update("""
            UPDATE questions
            SET deleted = 1
            WHERE id = #{questionId} AND owner_user_id = #{userId} AND source_type = 'USER_UPLOAD' AND deleted = 0
            """)
    int deleteMine(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 逻辑删除当前用户全部个人题目。
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("""
            UPDATE questions
            SET deleted = 1
            WHERE owner_user_id = #{userId} AND source_type = 'USER_UPLOAD' AND deleted = 0
            """)
    int deleteAllMine(@Param("userId") Long userId);
}
