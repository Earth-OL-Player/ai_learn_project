package com.earth.online.player.ailearn.analysis.interfaces;

import java.util.List;

/**
 * 学习分析响应。
 *
 * @param weakPoints 薄弱知识点
 */
public record LearningAnalysisResponse(List<WeakPointResponse> weakPoints) {
}
