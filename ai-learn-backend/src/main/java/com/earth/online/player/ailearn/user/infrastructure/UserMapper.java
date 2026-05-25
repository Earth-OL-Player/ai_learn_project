package com.earth.online.player.ailearn.user.infrastructure;

import com.earth.online.player.ailearn.user.domain.User;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 MyBatis 仓储。
 */
@Mapper
public interface UserMapper {

    /**
     * 统计未删除用户数。
     *
     * @return 用户数
     */
    @Select("SELECT COUNT(1) FROM users WHERE deleted = 0")
    long countActiveUsers();

    /**
     * 管理端分页查询用户。
     *
     * @param keyword 关键词
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 用户列表
     */
    @Select("""
            <script>
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE deleted = 0
            <if test='keyword != null and keyword != ""'>
              AND (username LIKE CONCAT('%', #{keyword}, '%')
                   OR nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR email LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY id ASC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<User> findAdminPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 管理端统计用户数量。
     *
     * @param keyword 关键词
     * @return 用户数量
     */
    @Select("""
            <script>
            SELECT COUNT(1)
            FROM users
            WHERE deleted = 0
            <if test='keyword != null and keyword != ""'>
              AND (username LIKE CONCAT('%', #{keyword}, '%')
                   OR nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR email LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    long countAdminPage(@Param("keyword") String keyword);

    /**
     * 根据用户ID查询未删除用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE id = #{id} AND deleted = 0
            """)
    User findById(@Param("id") Long id);

    /**
     * 根据用户ID加锁查询未删除用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE id = #{id} AND deleted = 0
            FOR UPDATE
            """)
    User findByIdForUpdate(@Param("id") Long id);

    /**
     * 根据用户名查询未删除用户。
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE username = #{username} AND deleted = 0
            """)
    User findByUsername(@Param("username") String username);

    /**
     * 根据用户名或邮箱查询未删除用户。
     *
     * @param usernameOrEmail 用户名或邮箱
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE (username = #{usernameOrEmail} OR email = #{usernameOrEmail}) AND deleted = 0
            LIMIT 1
            """)
    User findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    /**
     * 根据昵称查询未删除用户。
     *
     * @param nickname 昵称
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE nickname = #{nickname} AND deleted = 0
            """)
    User findByNickname(@Param("nickname") String nickname);

    /**
     * 根据邮箱查询未删除用户。
     *
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE email = #{email} AND deleted = 0
            """)
    User findByEmail(@Param("email") String email);

    /**
     * 根据用户名查询用户，包含已删除数据。
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE username = #{username}
            LIMIT 1
            """)
    User findByUsernameAny(@Param("username") String username);

    /**
     * 根据昵称查询用户，包含已删除数据。
     *
     * @param nickname 昵称
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE nickname = #{nickname}
            LIMIT 1
            """)
    User findByNicknameAny(@Param("nickname") String nickname);

    /**
     * 根据邮箱查询用户，包含已删除数据。
     *
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin,
                   created_at, updated_at, deleted
            FROM users
            WHERE email = #{email}
            LIMIT 1
            """)
    User findByEmailAny(@Param("email") String email);

    /**
     * 新增用户。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO users(username, nickname, avatar, gender, motto, email, password_hash, experience, level_code, rank_code, super_admin)
            VALUES(#{username}, #{nickname}, #{avatar}, #{gender}, #{motto}, #{email}, #{passwordHash}, #{experience}, #{levelCode}, #{rankCode}, #{superAdmin})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(User user);

    /**
     * 更新用户成长字段。
     *
     * @param id 用户ID
     * @param experience 经验值
     * @param levelCode 等级编码
     * @param rankCode 段位编码
     * @return 影响行数
     */
    @Update("""
            UPDATE users
            SET experience = #{experience}, level_code = #{levelCode}, rank_code = #{rankCode}
            WHERE id = #{id} AND deleted = 0
            """)
    int updateGrowth(
            @Param("id") Long id,
            @Param("experience") int experience,
            @Param("levelCode") String levelCode,
            @Param("rankCode") String rankCode
    );

    /**
     * 更新当前用户资料。
     *
     * @param id 用户ID
     * @param nickname 昵称
     * @param gender 性别编码
     * @param motto 用户座右铭
     * @return 影响行数
     */
    @Update("""
            UPDATE users
            SET nickname = #{nickname},
                gender = #{gender},
                motto = #{motto}
            WHERE id = #{id} AND deleted = 0
            """)
    int updateProfile(
            @Param("id") Long id,
            @Param("nickname") String nickname,
            @Param("gender") String gender,
            @Param("motto") String motto
    );

    /**
     * 管理端更新用户基础信息。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Update("""
            UPDATE users
            SET username = #{username},
                nickname = #{nickname},
                avatar = #{avatar},
                email = #{email},
                password_hash = CASE WHEN #{passwordHash} IS NULL THEN password_hash ELSE #{passwordHash} END,
                super_admin = #{superAdmin}
            WHERE id = #{id} AND deleted = 0
            """)
    int updateByAdmin(User user);

    /**
     * 管理端逻辑删除用户。
     *
     * @param id 用户ID
     * @return 影响行数
     */
    @Update("UPDATE users SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int softDeleteById(@Param("id") Long id);

}
