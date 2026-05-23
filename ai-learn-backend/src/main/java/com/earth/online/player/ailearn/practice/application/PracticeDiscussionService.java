package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.growth.application.GrowthAwardService;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeAiClient;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeSessionRecord;
import com.earth.online.player.ailearn.practice.interfaces.PracticeMessageResponse;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

/**
 * 当前题 AI 讨论服务。
 */
@Service
public class PracticeDiscussionService {

    private final PracticeSessionService practiceSessionService;
    private final DiscussionHistoryService discussionHistoryService;
    private final PracticeResponseAssembler responseAssembler;
    private final GrowthAwardService growthAwardService;
    private final PracticeAiClient practiceAiClient;

    /**
     * 创建当前题 AI 讨论服务。
     *
     * @param practiceSessionService 会话状态服务
     * @param discussionHistoryService 讨论历史服务
     * @param responseAssembler 响应组装服务
     * @param growthAwardService 勋章发放服务
     * @param practiceAiClient AI 服务客户端
     */
    public PracticeDiscussionService(
            PracticeSessionService practiceSessionService,
            DiscussionHistoryService discussionHistoryService,
            PracticeResponseAssembler responseAssembler,
            GrowthAwardService growthAwardService,
            PracticeAiClient practiceAiClient) {
        this.practiceSessionService = practiceSessionService;
        this.discussionHistoryService = discussionHistoryService;
        this.responseAssembler = responseAssembler;
        this.growthAwardService = growthAwardService;
        this.practiceAiClient = practiceAiClient;
    }

    /**
     * 围绕当前题进行 AI 讨论。
     *
     * @param userId 用户ID
     * @param question 当前题目
     * @param content 消息内容
     * @param chunkConsumer 文本片段处理器
     * @return 讨论响应
     */
    public PracticeMessageResponse discussCurrentQuestion(
            Long userId,
            PracticeQuestionRecord question,
            String content,
            Consumer<String> chunkConsumer) {
        PracticeSessionRecord session = practiceSessionService.findSession(userId);
        String lastUserAnswer = session == null ? "" : session.getLastAnswerText();
        String gradingSummary = session == null ? "" : session.getLastGradingSummary();
        String historyJson = session == null ? "" : session.getDiscussionHistoryJson();
        String reply = practiceAiClient.discussStream(question, lastUserAnswer, gradingSummary, historyJson, content, chunkConsumer)
                .orElse(PracticeConstants.FALLBACK_DISCUSSION_MESSAGE);
        discussionHistoryService.saveDiscussionHistory(userId, historyJson, content, reply);

        // 追问类勋章通过成长信息透传给前端，评分结果为空时仍可弹框提示。
        int followUpCount = practiceSessionService.incrementDiscussionFollowUpCount(userId);
        List<BadgeResponse> newBadges = growthAwardService.awardAfterDiscussion(userId, followUpCount);
        return responseAssembler.discussionResponse(question, reply, newBadges);
    }
}
