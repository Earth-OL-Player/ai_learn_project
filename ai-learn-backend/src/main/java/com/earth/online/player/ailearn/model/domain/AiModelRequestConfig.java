package com.earth.online.player.ailearn.model.domain;

/**
 * AI 服务请求级模型配置。
 *
 * @param model 模型名称
 * @param baseUrl 模型基础地址
 * @param apiKey 模型 API Key
 * @param configFingerprint 模型配置指纹
 */
public record AiModelRequestConfig(String model, String baseUrl, String apiKey, String configFingerprint) {
}
