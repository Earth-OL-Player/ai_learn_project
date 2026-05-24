package com.earth.online.player.ailearn.model.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模型授权入口配置。
 */
@ConfigurationProperties(prefix = "app.model-authorization")
public class ModelAuthorizationProperties {

    private String url = "";

    /**
     * 获取授权入口地址。
     *
     * @return 授权入口地址
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置授权入口地址。
     *
     * @param url 授权入口地址
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
