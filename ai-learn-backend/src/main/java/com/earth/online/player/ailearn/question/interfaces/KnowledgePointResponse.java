package com.earth.online.player.ailearn.question.interfaces;

/**
 * 知识点响应对象。
 *
 * @param id 知识点ID
 * @param name 知识点名称
 * @param description 知识点说明
 */
public record KnowledgePointResponse(String id, String name, String description) {
}
