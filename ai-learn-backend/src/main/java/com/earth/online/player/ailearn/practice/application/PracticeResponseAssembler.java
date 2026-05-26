package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.growth.application.GrowthService;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeSessionRecord;
import com.earth.online.player.ailearn.practice.interfaces.PracticeGradingResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeMessageResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeQuestionResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeStateResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 刷题响应组装服务。
 */
@Service
public class PracticeResponseAssembler {

    private final GrowthService growthService;
    private final PracticeChatHistoryService practiceChatHistoryService;

    /**
     * 创建刷题响应组装服务。
     *
     * @param growthService 成长服务
     * @param practiceChatHistoryService 跨端展示聊天记录服务
     */
    public PracticeResponseAssembler(GrowthService growthService, PracticeChatHistoryService practiceChatHistoryService) {
        this.growthService = growthService;
        this.practiceChatHistoryService = practiceChatHistoryService;
    }

    /**
     * 构造当前刷题状态响应。
     *
     * @param session 当前会话
     * @param question 当前题
     * @param questionTypes 题目分类
     * @return 状态响应
     */
    public PracticeStateResponse stateResponse(
            PracticeSessionRecord session,
            PracticeQuestionRecord question,
            List<String> questionTypes) {
        String phase = session == null ? PracticeConstants.PHASE_QUESTIONING : session.getPhase();
        return new PracticeStateResponse(
                phase,
                phaseText(phase),
                question == null ? null : toQuestionResponse(question),
                session == null ? null : session.getLastScore(),
                questionTypes,
                session == null ? List.of() : practiceChatHistoryService.readChatHistory(session.getChatHistoryJson()),
                growthService.getCurrentGrowth()
        );
    }

    /**
     * 构造出题响应。
     *
     * @param question 题目
     * @param message 提示消息
     * @return 出题响应
     */
    public PracticeMessageResponse questionResponse(PracticeQuestionRecord question, String message) {
        return new PracticeMessageResponse(
                PracticeConstants.ACTION_QUESTION,
                PracticeConstants.PHASE_ANSWERING,
                message,
                toQuestionResponse(question),
                null,
                growthService.getCurrentGrowth()
        );
    }

    /**
     * 构造提示响应。
     *
     * @param phase 当前阶段
     * @param message 提示消息
     * @return 提示响应
     */
    public PracticeMessageResponse tip(String phase, String message) {
        return new PracticeMessageResponse(
                PracticeConstants.ACTION_TIP,
                phase,
                message,
                null,
                null,
                growthService.getCurrentGrowth()
        );
    }

    /**
     * 构造评分响应消息。
     *
     * @param question 题目
     * @param grading 评分结果
     * @param fallbackUsed 是否使用兜底评分
     * @return 评分响应消息
     */
    public PracticeMessageResponse gradingResponse(
            PracticeQuestionRecord question,
            PracticeGradingResponse grading,
            boolean fallbackUsed) {
        return new PracticeMessageResponse(
                PracticeConstants.ACTION_GRADING,
                PracticeConstants.PHASE_DISCUSSING,
                buildGradingMessage(fallbackUsed),
                toQuestionResponse(question),
                grading,
                growthService.getCurrentGrowth(grading.newBadges())
        );
    }

    /**
     * 构造讨论响应。
     *
     * @param question 当前题目
     * @param reply 回复内容
     * @param newBadges 新获得勋章
     * @return 讨论响应
     */
    public PracticeMessageResponse discussionResponse(
            PracticeQuestionRecord question,
            String reply,
            List<BadgeResponse> newBadges) {
        return new PracticeMessageResponse(
                PracticeConstants.ACTION_DISCUSSION,
                PracticeConstants.PHASE_DISCUSSING,
                reply,
                toQuestionResponse(question),
                null,
                growthService.getCurrentGrowth(newBadges)
        );
    }

    /**
     * 构造当前题评分摘要。
     *
     * @param grading 评分响应
     * @return 评分摘要
     */
    public String buildGradingSummary(PracticeGradingResponse grading) {
        StringBuilder builder = new StringBuilder();
        builder.append("本次评分：").append(grading.score()).append("分，")
                .append(scoreLevelText(grading.score())).append("。");

        // 评分摘要用于后续追问上下文，不需要保留全部前端展示字段。
        appendSummaryList(builder, "命中点", grading.hitPoints());
        appendSummaryList(builder, "缺失点", grading.missingPoints());
        appendSummaryList(builder, "问题点", grading.problems());
        builder.append("优化建议：").append(grading.improvementAdvice()).append("。");
        builder.append("评分来源：").append(Boolean.TRUE.equals(grading.fallbackUsed()) ? "本地兜底评分" : "AI评分").append("。");
        return limitText(builder.toString(), PracticeConstants.MAX_GRADING_SUMMARY_LENGTH);
    }

    /**
     * 转换题目响应。
     *
     * @param question 题目记录
     * @return 响应对象
     */
    public PracticeQuestionResponse toQuestionResponse(PracticeQuestionRecord question) {
        return new PracticeQuestionResponse(
                question.getCode(),
                question.getQuestion(),
                question.getQuestionType(),
                question.getImportanceScore(),
                question.getOccurrenceCount(),
                safeInt(question.getAnsweredCount()),
                safeInt(question.getBestScore())
        );
    }

    /**
     * 构造评分完成提示。
     *
     * @param fallbackUsed 是否使用兜底评分
     * @return 评分完成提示
     */
    private String buildGradingMessage(boolean fallbackUsed) {
        if (fallbackUsed) {
            return "评分完成。当前大模型调用异常，已使用本地兜底策略完成评分和解析。您可以重新作答或者开始下一题。";
        }
        return "评分完成。您可以与我探讨细节、重新作答或者开始下一题。";
    }

    /**
     * 根据分数转换评分等级文案。
     *
     * @param score 分数
     * @return 等级文案
     */
    private String scoreLevelText(Integer score) {
        int safeScore = score == null ? 0 : score;
        if (safeScore < PracticeConstants.PASS_SCORE) {
            return "继续加油";
        }
        if (safeScore < PracticeConstants.EXCELLENT_SCORE) {
            return "合格答案";
        }
        return "非常棒";
    }

    /**
     * 追加摘要列表。
     *
     * @param builder 摘要构造器
     * @param title 列表标题
     * @param values 列表值
     */
    private void appendSummaryList(StringBuilder builder, String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        // 列表字段使用顿号连接，减少多轮上下文长度。
        builder.append(title).append("：").append(String.join("、", values)).append("。");
    }

    /**
     * 转换阶段文案。
     *
     * @param phase 阶段编码
     * @return 阶段文案
     */
    private String phaseText(String phase) {
        if (PracticeConstants.PHASE_ANSWERING.equals(phase)) {
            return "答题中";
        }
        if (PracticeConstants.PHASE_DISCUSSING.equals(phase)) {
            return "本题讨论中";
        }
        return "等待出题";
    }

    /**
     * 限制文本长度。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        // 截断后保留明确提示，便于模型理解上下文被压缩过。
        return text.substring(0, maxLength) + "……";
    }

    /**
     * 获取安全整数。
     *
     * @param value 原始值
     * @return 安全值
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
