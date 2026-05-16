package com.earth.online.player.ailearn.question.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 系统题库管理 MyBatis 仓储。
 */
@Mapper
public interface SystemQuestionAdminMapper {

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
            SELECT id, code, question, question_type, standard_answer, importance_score,
                   occurrence_count, created_at, updated_at, deleted
            FROM questions
            WHERE deleted = 0
            <if test='keyword != null and keyword != ""'>
              AND (code LIKE CONCAT('%', #{keyword}, '%')
                   OR question LIKE CONCAT('%', #{keyword}, '%')
                   OR standard_answer LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='questionType != null and questionType != ""'>
              AND question_type = #{questionType}
            </if>
            ORDER BY updated_at DESC, id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<SystemQuestionRecord> findPage(
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
     * @return 总数
     */
    @Select("""
            <script>
            SELECT COUNT(1)
            FROM questions
            WHERE deleted = 0
            <if test='keyword != null and keyword != ""'>
              AND (code LIKE CONCAT('%', #{keyword}, '%')
                   OR question LIKE CONCAT('%', #{keyword}, '%')
                   OR standard_answer LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='questionType != null and questionType != ""'>
              AND question_type = #{questionType}
            </if>
            </script>
            """)
    long countPage(@Param("keyword") String keyword, @Param("questionType") String questionType);

    /**
     * 按ID查询题目。
     *
     * @param id 题目ID
     * @return 题目记录
     */
    @Select("""
            SELECT id, code, question, question_type, standard_answer, importance_score,
                   occurrence_count, created_at, updated_at, deleted
            FROM questions
            WHERE id = #{id} AND deleted = 0
            """)
    SystemQuestionRecord findById(@Param("id") Long id);

    /**
     * 按编码查询题目，包含已删除数据。
     *
     * @param code 题目编码
     * @return 题目记录
     */
    @Select("""
            SELECT id, code, question, question_type, standard_answer, importance_score,
                   occurrence_count, created_at, updated_at, deleted
            FROM questions
            WHERE code = #{code}
            LIMIT 1
            """)
    SystemQuestionRecord findByCodeAny(@Param("code") String code);

    /**
     * 查询全部题目分类。
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
     * 新增系统题目。
     *
     * @param record 写入记录
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO questions(code, question, question_type, standard_answer, importance_score, occurrence_count)
            VALUES(#{code}, #{question}, #{questionType}, #{standardAnswer}, #{importanceScore}, #{occurrenceCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(SystemQuestionWriteRecord record);

    /**
     * 更新系统题目并恢复逻辑删除状态。
     *
     * @param record 写入记录
     * @return 影响行数
     */
    @Update("""
            UPDATE questions
            SET code = #{code},
                question = #{question},
                question_type = #{questionType},
                standard_answer = #{standardAnswer},
                importance_score = #{importanceScore},
                occurrence_count = #{occurrenceCount},
                deleted = 0
            WHERE id = #{id}
            """)
    int update(SystemQuestionWriteRecord record);

    /**
     * 逻辑删除系统题目。
     *
     * @param id 题目ID
     * @return 影响行数
     */
    @Update("UPDATE questions SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int deleteById(@Param("id") Long id);
}
