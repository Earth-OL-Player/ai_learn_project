package com.earth.online.player.ailearn.learning.dto;

import java.util.List;

/**
 * 学习路线响应数据。
 *
 * @param title 标题
 * @param description 描述
 * @param platformIntro 平台说明
 * @param overview 路线总览
 * @param sections 路线阶段列表
 * @param resources 资料列表
 * @param suggestions 学习建议
 */
public record LearningRoadmapResponse(
        String title,
        String description,
        String platformIntro,
        String overview,
        List<RoadmapSection> sections,
        List<ResourceItem> resources,
        List<String> suggestions
) {
}
