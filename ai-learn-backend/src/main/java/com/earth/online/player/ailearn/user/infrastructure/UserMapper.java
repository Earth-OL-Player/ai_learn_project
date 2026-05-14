package com.earth.online.player.ailearn.user.infrastructure;

import com.earth.online.player.ailearn.user.domain.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 MyBatis 仓储。
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户ID查询未删除用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, email, password_hash, experience, level_code, rank_code,
                   created_at, updated_at, deleted
            FROM users
            WHERE id = #{id} AND deleted = 0
            """)
    User findById(@Param("id") Long id);

    /**
     * 根据用户名查询未删除用户。
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, email, password_hash, experience, level_code, rank_code,
                   created_at, updated_at, deleted
            FROM users
            WHERE username = #{username} AND deleted = 0
            """)
    User findByUsername(@Param("username") String username);

    /**
     * 根据昵称查询未删除用户。
     *
     * @param nickname 昵称
     * @return 用户信息
     */
    @Select("""
            SELECT id, username, nickname, avatar, email, password_hash, experience, level_code, rank_code,
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
            SELECT id, username, nickname, avatar, email, password_hash, experience, level_code, rank_code,
                   created_at, updated_at, deleted
            FROM users
            WHERE email = #{email} AND deleted = 0
            """)
    User findByEmail(@Param("email") String email);

    /**
     * 新增用户。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO users(username, nickname, avatar, email, password_hash, experience, level_code, rank_code)
            VALUES(#{username}, #{nickname}, #{avatar}, #{email}, #{passwordHash}, #{experience}, #{levelCode}, #{rankCode})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(User user);
}
