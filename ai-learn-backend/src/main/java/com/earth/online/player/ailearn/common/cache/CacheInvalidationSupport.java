package com.earth.online.player.ailearn.common.cache;

import java.util.Objects;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 缓存失效事务辅助工具。
 */
public final class CacheInvalidationSupport {

    private CacheInvalidationSupport() {
    }

    /**
     * 在事务提交后执行失效动作；无事务时立即执行。
     *
     * @param invalidation 缓存失效动作
     */
    public static void afterCommit(Runnable invalidation) {
        Objects.requireNonNull(invalidation, "缓存失效动作不能为空");
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invalidation.run();
            return;
        }

        // 只有数据库提交成功后才清缓存，避免回滚事务让下一次读取提前拿到旧值。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invalidation.run();
            }
        });
    }
}
