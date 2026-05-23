package com.earth.online.player.ailearn.common.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * 内存限流服务。
 */
@Service
public class InMemoryRateLimitService {

    private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final long COUNTER_TTL_MILLIS = Duration.ofDays(1).toMillis();
    private static final String KEY_SEPARATOR = ":";

    private final Map<String, WindowCounter> frequencyCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> concurrentCounters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMillis = new AtomicLong(System.currentTimeMillis());

    /**
     * 检查并消耗一次固定窗口频率额度。
     *
     * @param ruleName 规则名称
     * @param identity 限流标识
     * @param maxRequests 窗口内最大请求数
     * @param windowSeconds 窗口秒数
     */
    public void checkFrequency(String ruleName, String identity, int maxRequests, int windowSeconds) {
        if (maxRequests <= 0 || windowSeconds <= 0) {
            throw new RateLimitExceededException("当前操作暂时不可用，请稍后重试");
        }

        // 定期清理过期窗口，避免内存级过渡方案长期运行后堆积无效键。
        long nowMillis = System.currentTimeMillis();
        cleanupExpiredCounters(nowMillis);

        String counterKey = buildCounterKey(ruleName, identity);
        WindowCounter counter = frequencyCounters.computeIfAbsent(counterKey, key -> new WindowCounter(nowMillis));
        long retryAfterSeconds = counter.consume(maxRequests, Duration.ofSeconds(windowSeconds).toMillis(), nowMillis);
        if (retryAfterSeconds > 0) {
            throw new RateLimitExceededException("请求过于频繁，请 " + retryAfterSeconds + " 秒后重试");
        }
    }

    /**
     * 获取并发占用租约。
     *
     * @param ruleName 规则名称
     * @param identity 限流标识
     * @param maxConcurrent 最大并发数
     * @return 并发租约
     */
    public RateLimitLease acquireConcurrency(String ruleName, String identity, int maxConcurrent) {
        if (maxConcurrent <= 0) {
            throw new RateLimitExceededException("当前操作暂时不可用，请稍后重试");
        }

        String counterKey = buildCounterKey(ruleName, identity);
        AtomicInteger counter = concurrentCounters.computeIfAbsent(counterKey, key -> new AtomicInteger(0));
        while (true) {
            int current = counter.get();
            if (current >= maxConcurrent) {
                throw new RateLimitExceededException("当前已有 AI 请求处理中，请等待完成后再试");
            }
            if (counter.compareAndSet(current, current + 1)) {
                return new RateLimitLease(() -> releaseConcurrency(counterKey, counter));
            }
        }
    }

    /**
     * 释放并发占用。
     *
     * @param counterKey 计数键
     * @param counter 并发计数器
     */
    private void releaseConcurrency(String counterKey, AtomicInteger counter) {
        int current = counter.decrementAndGet();
        if (current <= 0) {
            concurrentCounters.remove(counterKey, counter);
        }
    }

    /**
     * 构造限流计数键。
     *
     * @param ruleName 规则名称
     * @param identity 限流标识
     * @return 计数键
     */
    private String buildCounterKey(String ruleName, String identity) {
        return ruleName + KEY_SEPARATOR + identity;
    }

    /**
     * 清理过期频率窗口。
     *
     * @param nowMillis 当前时间毫秒
     */
    private void cleanupExpiredCounters(long nowMillis) {
        long previousCleanupMillis = lastCleanupMillis.get();
        if (nowMillis - previousCleanupMillis < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        if (!lastCleanupMillis.compareAndSet(previousCleanupMillis, nowMillis)) {
            return;
        }

        // 仅清理已自然过期一段时间的窗口，避免影响正在统计的当前窗口。
        frequencyCounters.entrySet().removeIf(entry -> entry.getValue().isExpired(nowMillis, COUNTER_TTL_MILLIS));
    }

    /**
     * 固定窗口计数器。
     */
    private static final class WindowCounter {

        private long windowStartMillis;
        private int count;

        /**
         * 创建窗口计数器。
         *
         * @param windowStartMillis 窗口开始时间
         */
        private WindowCounter(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }

        /**
         * 消耗一次请求额度。
         *
         * @param maxRequests 最大请求数
         * @param windowMillis 窗口毫秒数
         * @param nowMillis 当前时间毫秒
         * @return 允许时返回0，超限时返回等待秒数
         */
        private synchronized long consume(int maxRequests, long windowMillis, long nowMillis) {
            if (nowMillis - windowStartMillis >= windowMillis) {
                windowStartMillis = nowMillis;
                count = 0;
            }
            if (count >= maxRequests) {
                return Math.max(1L, (windowMillis - (nowMillis - windowStartMillis) + 999L) / 1000L);
            }

            // 只有未超限请求会增加计数，避免被拒绝请求持续推高等待时间。
            count++;
            return 0L;
        }

        /**
         * 判断窗口是否已过期。
         *
         * @param nowMillis 当前时间毫秒
         * @param ttlMillis 额外保留时间毫秒
         * @return 是否过期
         */
        private synchronized boolean isExpired(long nowMillis, long ttlMillis) {
            return nowMillis - windowStartMillis > ttlMillis;
        }
    }
}
