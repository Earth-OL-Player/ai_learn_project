package com.earth.online.player.ailearn.learning.dto;

import java.util.List;

/**
 * 学习路线阶段。
 *
 * @param title 阶段标题
 * @param summary 阶段摘要
 * @param items 阶段学习项
 */
public record RoadmapSection(String title, String summary, List<String> items) {
}
