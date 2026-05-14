package com.earth.online.player.ailearn.learning.dto;

/**
 * 学习资料条目。
 *
 * @param title 资料标题
 * @param description 资料说明
 * @param url 资料地址
 */
public record ResourceItem(String title, String description, String url) {
}
