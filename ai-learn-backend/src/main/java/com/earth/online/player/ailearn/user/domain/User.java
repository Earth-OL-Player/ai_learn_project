package com.earth.online.player.ailearn.user.domain;

import java.time.LocalDateTime;

/**
 * 用户领域对象。
 */
public class User {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String gender;
    private String email;
    private String passwordHash;
    private Integer experience;
    private String levelCode;
    private String rankCode;
    private Boolean superAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;

    /**
     * 获取用户ID。
     *
     * @return 用户ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户ID。
     *
     * @param id 用户ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取昵称。
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称。
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取头像。
     *
     * @return 头像地址
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * 设置头像。
     *
     * @param avatar 头像地址
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * 获取性别编码。
     *
     * @return 性别编码
     */
    public String getGender() {
        return gender;
    }

    /**
     * 设置性别编码。
     *
     * @param gender 性别编码
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * 获取邮箱。
     *
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     *
     * @param email 邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取密码哈希。
     *
     * @return 密码哈希
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置密码哈希。
     *
     * @param passwordHash 密码哈希
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取经验值。
     *
     * @return 经验值
     */
    public Integer getExperience() {
        return experience;
    }

    /**
     * 设置经验值。
     *
     * @param experience 经验值
     */
    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    /**
     * 获取等级编码。
     *
     * @return 等级编码
     */
    public String getLevelCode() {
        return levelCode;
    }

    /**
     * 设置等级编码。
     *
     * @param levelCode 等级编码
     */
    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    /**
     * 获取段位编码。
     *
     * @return 段位编码
     */
    public String getRankCode() {
        return rankCode;
    }

    /**
     * 设置段位编码。
     *
     * @param rankCode 段位编码
     */
    public void setRankCode(String rankCode) {
        this.rankCode = rankCode;
    }

    /**
     * 获取超级管理员标识。
     *
     * @return 是否超级管理员
     */
    public Boolean getSuperAdmin() {
        return superAdmin;
    }

    /**
     * 设置超级管理员标识。
     *
     * @param superAdmin 是否超级管理员
     */
    public void setSuperAdmin(Boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取逻辑删除标识。
     *
     * @return 逻辑删除标识
     */
    public Boolean getDeleted() {
        return deleted;
    }

    /**
     * 设置逻辑删除标识。
     *
     * @param deleted 逻辑删除标识
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
