package com.earth.online.player.ailearn.common.config;

import com.earth.online.player.ailearn.ai.AiServiceProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AI/SSE 流式请求线程池配置。
 */
@Configuration
public class AiStreamExecutorConfig {

    /** AI 流式请求线程池 Bean 名称。 */
    public static final String AI_STREAM_EXECUTOR_BEAN_NAME = "aiStreamTaskExecutor";

    private static final String THREAD_NAME_PREFIX = "ai-sse-stream-";
    private static final String METRIC_PREFIX = "app.ai.stream.executor.";

    /**
     * 创建 AI/SSE 专用有界线程池。
     *
     * @param properties AI 服务配置
     * @return AI 流式请求线程池
     */
    @Bean(name = AI_STREAM_EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor aiStreamTaskExecutor(AiServiceProperties properties) {
        AiServiceProperties.StreamExecutor streamExecutor = properties.getStreamExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心参数全部有默认值并支持环境变量覆盖，避免流式请求进入公共线程池。
        executor.setCorePoolSize(atLeastOne(streamExecutor.getCoreSize()));
        executor.setMaxPoolSize(Math.max(atLeastOne(streamExecutor.getCoreSize()), streamExecutor.getMaxSize()));
        executor.setQueueCapacity(Math.max(0, streamExecutor.getQueueCapacity()));
        executor.setKeepAliveSeconds(Math.max(1, streamExecutor.getKeepAliveSeconds()));
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);

        // 关闭时等待短时间释放正在读流的任务，超时后由容器继续退出。
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(Math.max(1, streamExecutor.getAwaitTerminationSeconds()));
        executor.setRejectedExecutionHandler((runnable, poolExecutor) -> {
            AiStreamExecutorMetrics.REJECTED_TASKS.incrementAndGet();
            throw new RejectedExecutionException("AI 流式请求线程池已满");
        });
        executor.initialize();
        return executor;
    }

    /**
     * 注册 AI/SSE 线程池监控指标。
     *
     * @param executor AI 流式请求线程池
     * @return 指标绑定器
     */
    @Bean
    public MeterBinder aiStreamExecutorMeterBinder(
            @Qualifier(AI_STREAM_EXECUTOR_BEAN_NAME) ThreadPoolTaskExecutor executor) {
        return registry -> bindExecutorMetrics(registry, executor);
    }

    /**
     * 绑定线程池运行时指标。
     *
     * @param registry 指标注册表
     * @param executor AI 流式请求线程池
     */
    private void bindExecutorMetrics(MeterRegistry registry, ThreadPoolTaskExecutor executor) {
        Gauge.builder(METRIC_PREFIX + "active", executor, ThreadPoolTaskExecutor::getActiveCount).register(registry);
        Gauge.builder(METRIC_PREFIX + "pool.size", executor, ThreadPoolTaskExecutor::getPoolSize).register(registry);
        Gauge.builder(METRIC_PREFIX + "queue.size", executor, this::queueSize).register(registry);
        Gauge.builder(METRIC_PREFIX + "queue.remaining", executor, this::remainingQueueCapacity).register(registry);
        Gauge.builder(METRIC_PREFIX + "completed", executor, this::completedTaskCount).register(registry);
        Gauge.builder(METRIC_PREFIX + "rejected", AiStreamExecutorMetrics.REJECTED_TASKS, AtomicLong::get).register(registry);
    }

    /**
     * 读取线程池队列长度。
     *
     * @param executor AI 流式请求线程池
     * @return 队列长度
     */
    private int queueSize(ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor poolExecutor = executor.getThreadPoolExecutor();
        return poolExecutor.getQueue().size();
    }

    /**
     * 读取线程池队列剩余容量。
     *
     * @param executor AI 流式请求线程池
     * @return 剩余容量
     */
    private int remainingQueueCapacity(ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor poolExecutor = executor.getThreadPoolExecutor();
        return poolExecutor.getQueue().remainingCapacity();
    }

    /**
     * 读取线程池完成任务数。
     *
     * @param executor AI 流式请求线程池
     * @return 完成任务数
     */
    private long completedTaskCount(ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor poolExecutor = executor.getThreadPoolExecutor();
        return poolExecutor.getCompletedTaskCount();
    }

    /**
     * 将线程数修正为至少 1。
     *
     * @param value 原始值
     * @return 安全线程数
     */
    private int atLeastOne(int value) {
        return Math.max(1, value);
    }

    /**
     * AI 流式线程池指标状态。
     */
    private static final class AiStreamExecutorMetrics {

        private static final AtomicLong REJECTED_TASKS = new AtomicLong();

        private AiStreamExecutorMetrics() {
        }
    }
}
