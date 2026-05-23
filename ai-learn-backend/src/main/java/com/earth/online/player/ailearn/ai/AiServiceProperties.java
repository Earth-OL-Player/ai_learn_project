package com.earth.online.player.ailearn.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务调用配置。
 */
@ConfigurationProperties(prefix = "app.ai-service")
public class AiServiceProperties {

    private boolean enabled;
    private String baseUrl = "http://127.0.0.1:8000";
    private String token = "AI_SERVICE_TOKEN占位符";
    private int timeoutSeconds = 15;
    private StreamExecutor streamExecutor = new StreamExecutor();

    /**
     * 判断是否启用 AI 服务调用。
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 AI 服务调用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取 AI 服务基础地址。
     *
     * @return 服务地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置 AI 服务基础地址。
     *
     * @param baseUrl 服务地址
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 获取内部鉴权 Token。
     *
     * @return 鉴权 Token
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置内部鉴权 Token。
     *
     * @param token 鉴权 Token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取调用超时时间。
     *
     * @return 超时秒数
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 设置调用超时时间。
     *
     * @param timeoutSeconds 超时秒数
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 获取 AI 流式请求线程池配置。
     *
     * @return 线程池配置
     */
    public StreamExecutor getStreamExecutor() {
        return streamExecutor;
    }

    /**
     * 设置 AI 流式请求线程池配置。
     *
     * @param streamExecutor 线程池配置
     */
    public void setStreamExecutor(StreamExecutor streamExecutor) {
        this.streamExecutor = streamExecutor == null ? new StreamExecutor() : streamExecutor;
    }

    /**
     * AI/SSE 流式请求专用线程池配置。
     */
    public static class StreamExecutor {

        private int coreSize = 4;
        private int maxSize = 8;
        private int queueCapacity = 32;
        private int keepAliveSeconds = 60;
        private int awaitTerminationSeconds = 5;

        /**
         * 获取核心线程数。
         *
         * @return 核心线程数
         */
        public int getCoreSize() {
            return coreSize;
        }

        /**
         * 设置核心线程数。
         *
         * @param coreSize 核心线程数
         */
        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        /**
         * 获取最大线程数。
         *
         * @return 最大线程数
         */
        public int getMaxSize() {
            return maxSize;
        }

        /**
         * 设置最大线程数。
         *
         * @param maxSize 最大线程数
         */
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        /**
         * 获取等待队列容量。
         *
         * @return 队列容量
         */
        public int getQueueCapacity() {
            return queueCapacity;
        }

        /**
         * 设置等待队列容量。
         *
         * @param queueCapacity 队列容量
         */
        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        /**
         * 获取空闲线程保活秒数。
         *
         * @return 保活秒数
         */
        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        /**
         * 设置空闲线程保活秒数。
         *
         * @param keepAliveSeconds 保活秒数
         */
        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }

        /**
         * 获取停机等待秒数。
         *
         * @return 停机等待秒数
         */
        public int getAwaitTerminationSeconds() {
            return awaitTerminationSeconds;
        }

        /**
         * 设置停机等待秒数。
         *
         * @param awaitTerminationSeconds 停机等待秒数
         */
        public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
            this.awaitTerminationSeconds = awaitTerminationSeconds;
        }
    }
}
