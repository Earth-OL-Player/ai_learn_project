package com.earth.online.player.ailearn.interaction.application;

/**
 * 互动点赞切换器。
 */
public final class InteractionLikeToggler {

    /**
     * 工具类不允许实例化。
     */
    private InteractionLikeToggler() {
    }

    /**
     * 切换互动内容点赞状态。
     *
     * @param targetId 互动内容ID
     * @param userId 用户ID
     * @param insertLike 写入点赞明细操作
     * @param deleteLike 删除点赞明细操作
     * @param increaseLikeCount 增加点赞数操作
     * @param decreaseLikeCount 减少点赞数操作
     */
    public static void toggle(
            Long targetId,
            Long userId,
            LikeDetailWriter insertLike,
            LikeDetailWriter deleteLike,
            LikeCounterUpdater increaseLikeCount,
            LikeCounterUpdater decreaseLikeCount
    ) {
        int insertedRows = insertLike.apply(targetId, userId);
        if (insertedRows > 0) {
            increaseLikeCount.apply(targetId);
        } else if (deleteLike.apply(targetId, userId) > 0) {
            decreaseLikeCount.apply(targetId);
        }
    }

    /**
     * 点赞明细写入或删除操作。
     */
    @FunctionalInterface
    public interface LikeDetailWriter {

        /**
         * 执行点赞明细变更。
         *
         * @param targetId 互动内容ID
         * @param userId 用户ID
         * @return 影响行数
         */
        int apply(Long targetId, Long userId);
    }

    /**
     * 点赞计数更新操作。
     */
    @FunctionalInterface
    public interface LikeCounterUpdater {

        /**
         * 执行点赞计数更新。
         *
         * @param targetId 互动内容ID
         * @return 影响行数
         */
        int apply(Long targetId);
    }
}
