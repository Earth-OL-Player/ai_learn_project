package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.answer.domain.AnswerGradingPort;
import com.earth.online.player.ailearn.answer.domain.GradingResult;
import com.earth.online.player.ailearn.model.application.ModelEntitlementService;
import com.earth.online.player.ailearn.model.application.ResolvedModelEntitlement;
import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeAiClient;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeAiGradingResult;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.practice.interfaces.PracticeGradingResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeMessageResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 刷题答案评分和成长结算服务。
 */
@Service
public class PracticeGradingService {

    private final AnswerGradingPort answerGradingPort;
    private final PracticeAiClient practiceAiClient;
    private final PracticeGrowthSettlementService growthSettlementService;
    private final PracticeSessionService practiceSessionService;
    private final PracticeResponseAssembler responseAssembler;
    private final ModelEntitlementService modelEntitlementService;

    /**
     * 创建刷题答案评分和成长结算服务。
     *
     * @param answerGradingPort 本地评分端口
     * @param practiceAiClient AI 服务客户端
     * @param growthSettlementService 成长结算服务
     * @param practiceSessionService 会话状态服务
     * @param responseAssembler 响应组装服务
     * @param modelEntitlementService 模型权益服务
     */
    public PracticeGradingService(
            AnswerGradingPort answerGradingPort,
            PracticeAiClient practiceAiClient,
            PracticeGrowthSettlementService growthSettlementService,
            PracticeSessionService practiceSessionService,
            PracticeResponseAssembler responseAssembler,
            ModelEntitlementService modelEntitlementService) {
        this.answerGradingPort = answerGradingPort;
        this.practiceAiClient = practiceAiClient;
        this.growthSettlementService = growthSettlementService;
        this.practiceSessionService = practiceSessionService;
        this.responseAssembler = responseAssembler;
        this.modelEntitlementService = modelEntitlementService;
    }

    /**
     * 提交并评分答案。
     *
     * @param userId 用户ID
     * @param question 当前题目
     * @param userAnswer 用户答案
     * @return 评分响应
     */
    public PracticeMessageResponse submitAnswer(Long userId, PracticeQuestionRecord question, String userAnswer) {
        ResolvedModelEntitlement modelEntitlement = modelEntitlementService.resolveForAiCall(userId);
        Optional<PracticeAiGradingResult> aiGradingResult = practiceAiClient.grade(userId, question, userAnswer, modelEntitlement.requestConfig());
        boolean fallbackUsed = aiGradingResult.map(PracticeAiGradingResult::fallbackUsed).orElse(true);
        GradingResult gradingResult = aiGradingResult.map(PracticeAiGradingResult::gradingResult)
                .orElseGet(() -> answerGradingPort.grade(
                        userId,
                        question.getId(),
                        question.getQuestion(),
                        question.getStandardAnswer(),
                        List.of(question.getQuestionType()),
                        userAnswer
                ));
        PracticeGradingResponse grading = growthSettlementService.settleAnswer(userId, question, gradingResult, fallbackUsed);
        practiceSessionService.startDiscussing(
                userId,
                grading.score(),
                limitStoredAnswer(userAnswer),
                responseAssembler.buildGradingSummary(grading)
        );

        // 评分完成后进入本题讨论阶段，并仅保存当前题最近一次答案用于后续追问上下文。
        return responseAssembler.gradingResponse(question, grading, fallbackUsed);
    }

    /**
     * 限制当前题答案记忆长度。
     *
     * @param userAnswer 用户答案
     * @return 可保存答案
     */
    private String limitStoredAnswer(String userAnswer) {
        if (userAnswer.length() <= PracticeConstants.MAX_STORED_ANSWER_LENGTH) {
            return userAnswer;
        }

        // 仅保留足够追问的上下文，避免异常长答案撑大当前会话记录。
        return userAnswer.substring(0, PracticeConstants.MAX_STORED_ANSWER_LENGTH);
    }
}
