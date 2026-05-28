package com.earth.online.player.ailearn.common.cache;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Java 进程内短 TTL 缓存。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public final class LocalTtlCache<K, V> {

    private final long ttlNanos;
    private final ConcurrentHashMap<K, CacheEntry<V>> entries = new ConcurrentHashMap<>();

    /**
     * 创建本地 TTL 缓存。
     *
     * @param ttl 缓存有效期
     */
    public LocalTtlCache(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("缓存有效期必须大于0");
        }
        this.ttlNanos = ttl.toNanos();
    }

    /**
     * 获取缓存值，缓存不存在或过期时重新加载。
     *
     * @param key 缓存键
     * @param loader 数据加载器
     * @return 缓存值
     */
    public V get(K key, Supplier<V> loader) {
        Objects.requireNonNull(key, "缓存键不能为空");
        Objects.requireNonNull(loader, "缓存加载器不能为空");
        long now = System.nanoTime();
        CacheEntry<V> cachedEntry = entries.get(key);
        if (cachedEntry != null && !cachedEntry.isExpired(now)) {
            return cachedEntry.value();
        }

        // 同一个 key 的过期重载交给 ConcurrentHashMap 原子计算，避免并发重复查库。
        CacheEntry<V> refreshedEntry = entries.compute(key, (ignoredKey, existingEntry) -> {
            long refreshNow = System.nanoTime();
            if (existingEntry != null && !existingEntry.isExpired(refreshNow)) {
                return existingEntry;
            }
            V loadedValue = Objects.requireNonNull(loader.get(), "缓存加载结果不能为空");
            return new CacheEntry<>(loadedValue, refreshNow + ttlNanos);
        });
        return refreshedEntry.value();
    }

    /**
     * 失效指定缓存键。
     *
     * @param key 缓存键
     */
    public void invalidate(K key) {
        if (key != null) {
            entries.remove(key);
        }
    }

    /**
     * 清空全部缓存。
     */
    public void clear() {
        entries.clear();
    }

    /**
     * 单个缓存条目。
     *
     * @param value 缓存值
     * @param expireAtNanos 过期时间点
     * @param <V> 缓存值类型
     */
    private record CacheEntry<V>(V value, long expireAtNanos) {

        /**
         * 判断条目是否已经过期。
         *
         * @param nowNanos 当前时间点
         * @return 是否过期
         */
        private boolean isExpired(long nowNanos) {
            return nowNanos >= expireAtNanos;
        }
    }
}
