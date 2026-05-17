package com.earth.online.player.ailearn.interaction.domain;

/**
 * 内容作者安全摘要。
 *
 * @param id 用户ID
 * @param username 用户名
 * @param nickname 昵称
 * @param avatar 头像地址
 * @param level 等级展示编码
 * @param levelValue 等级数字
 * @param rank 段位名称
 */
public record AuthorSummary(
        String id,
        String username,
        String nickname,
        String avatar,
        String level,
        Integer levelValue,
        String rank
) {
}
