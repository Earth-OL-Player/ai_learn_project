package com.earth.online.player.ailearn.practice.interfaces;

import com.earth.online.player.ailearn.growth.interfaces.GrowthResponse;
import java.util.List;

/**
 * 刷题状态响应。
 *
 * @param phase 当前阶段
 * @param phaseText 当前阶段文案
 * @param currentQuestion 当前题目
 * @param lastScore 当前题最近得分
 * @param questionTypes 可选题目分类
 * @param growth 成长概览
 */
public record PracticeStateResponse(
        String phase,
        String phaseText,
        PracticeQuestionResponse currentQuestion,
        Integer lastScore,
        List<String> questionTypes,
        GrowthResponse growth
) {
}
