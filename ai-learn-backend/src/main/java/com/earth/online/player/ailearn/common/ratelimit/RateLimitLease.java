package com.earth.online.player.ailearn.common.ratelimit;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 限流并发占用租约。
 */
public final class RateLimitLease implements AutoCloseable {

    private final Runnable releaseAction;
    private final AtomicBoolean released = new AtomicBoolean(false);

    /**
     * 创建并发租约。
     *
     * @param releaseAction 释放动作
     */
    public RateLimitLease(Runnable releaseAction) {
        this.releaseAction = releaseAction;
    }

    /**
     * 释放当前并发占用。
     */
    @Override
    public void close() {
        if (released.compareAndSet(false, true)) {
            releaseAction.run();
        }
    }
}
