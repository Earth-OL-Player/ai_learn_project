package com.earth.online.player.ailearn.model.interfaces;

/**
 * 管理员模型配置保存请求。
 *
 * @param modelName 模型名称
 * @param baseUrl 模型基础地址
 * @param apiKey 模型 API Key
 */
public record AdminModelConfigRequest(String modelName, String baseUrl, String apiKey) {
}
