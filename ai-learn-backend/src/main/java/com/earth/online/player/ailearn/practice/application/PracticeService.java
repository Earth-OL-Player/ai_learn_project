package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeSessionRecord;
import com.earth.online.player.ailearn.practice.interfaces.PracticeActionRequest;
import com.earth.online.player.ailearn.practice.interfaces.PracticeMessageRequest;
import com.earth.online.player.ailearn.practice.interfaces.PracticeMessageResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeStateResponse;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 智能刷题应用编排服务。
 */
@Service
public class PracticeService {

    private final PracticeSessionService practiceSessionService;
    private final QuestionSelectionService questionSelectionService;
    private final PracticeGradingService practiceGradingService;
    private final PracticeDiscussionService practiceDiscussionService;
    private final PracticeChatHistoryService practiceChatHistoryService;
    private final PracticeResponseAssembler responseAssembler;
    private final PracticeMessageClassifier messageClassifier;

    /**
     * 创建 AI 智能刷题应用编排服务。
     *
     * @param practiceSessionService 会话状态服务
     * @param questionSelectionService 抽题策略服务
     * @param practiceGradingService 评分成长服务
     * @param practiceDiscussionService 当前题 AI 讨论服务
     * @param practiceChatHistoryService 跨端展示聊天记录服务
     * @param responseAssembler 响应组装服务
     * @param messageClassifier 消息意图识别服务
     */
    public PracticeService(
            PracticeSessionService practiceSessionService,
            QuestionSelectionService questionSelectionService,
            PracticeGradingService practiceGradingService,
            PracticeDiscussionService practiceDiscussionService,
            PracticeChatHistoryService practiceChatHistoryService,
            PracticeResponseAssembler responseAssembler,
            PracticeMessageClassifier messageClassifier) {
        this.practiceSessionService = practiceSessionService;
        this.questionSelectionService = questionSelectionService;
        this.practiceGradingService = practiceGradingService;
        this.practiceDiscussionService = practiceDiscussionService;
        this.practiceChatHistoryService = practiceChatHistoryService;
        this.responseAssembler = responseAssembler;
        this.messageClassifier = messageClassifier;
    }

    /**
     * 查询题目分类。
     *
     * @return 分类列表
     */
    public List<String> findQuestionTypes() {
        currentUserId();
        return questionSelectionService.findQuestionTypes();
    }

    /**
     * 查询当前刷题状态。
     *
     * @return 当前状态
     */
    public PracticeStateResponse getState() {
        Long userId = currentUserId();
        PracticeSessionRecord session = practiceSessionService.findSession(userId);
        PracticeQuestionRecord question = practiceSessionService.findSessionQuestion(userId, session);

        // 状态接口只负责聚合当前进度，不改变任何会话阶段。
        return responseAssembler.stateResponse(session, question, questionSelectionService.findQuestionTypes());
    }

    /**
     * 抽取下一题。
     *
     * @param request 出题请求
     * @return 出题响应
     */
    @Transactional
    public PracticeMessageResponse nextQuestion(PracticeActionRequest request) {
        Long userId = currentUserId();
        List<String> questionTypes = questionSelectionService.normalizeTypes(request == null ? null : request.questionTypes());
        PracticeQuestionRecord question = questionSelectionService.selectQuestion(userId, questionTypes);
        practiceSessionService.startAnswering(userId, question);
        PracticeMessageResponse response = responseAssembler.questionResponse(question, "已为你抽取一道新题，请认真作答。答完后我会给出评分和建议。");
        practiceChatHistoryService.replaceWithAssistantMessage(userId, response);
        return response;
    }

    /**
     * 重新回答当前题。
     *
     * @return 重新回答响应
     */
    @Transactional
    public PracticeMessageResponse retryCurrentQuestion() {
        Long userId = currentUserId();
        PracticeMessageResponse response = buildRetryCurrentQuestionResponse(userId);
        practiceChatHistoryService.replaceWithAssistantMessage(userId, response);
        return response;
    }

    /**
     * 处理聊天消息并尽可能流式输出讨论回复。
     *
     * @param request 聊天请求
     * @param chunkConsumer 文本片段处理器
     * @return 聊天响应
     */
    @Transactional
    public PracticeMessageResponse handleMessageStream(PracticeMessageRequest request, Consumer<String> chunkConsumer) {
        Long userId = currentUserId();
        String content = messageClassifier.normalizeContent(request == null ? null : request.content());
        PracticeSessionRecord session = practiceSessionService.findSession(userId);
        String phase = session == null ? PracticeConstants.PHASE_QUESTIONING : session.getPhase();
        String previousQuestionCode = session == null ? null : session.getQuestionCode();

        // 出题和评分仍需要完整业务结果；讨论阶段优先使用真实模型流式输出。
        PracticeMessageResponse response;
        if (PracticeConstants.PHASE_QUESTIONING.equals(phase)) {
            response = handleQuestioningMessage(userId, content, request);
        } else if (PracticeConstants.PHASE_ANSWERING.equals(phase)) {
            response = handleAnsweringMessage(userId, content);
        } else {
            response = handleDiscussingMessageStream(userId, content, request, chunkConsumer);
        }
        syncChatHistoryAfterStreamMessage(userId, content, phase, previousQuestionCode, response);
        return response;
    }

    /**
     * 处理出题阶段消息。
     *
     * @param userId 用户ID
     * @param content 消息内容
     * @param request 原始请求
     * @return 响应
     */
    private PracticeMessageResponse handleQuestioningMessage(Long userId, String content, PracticeMessageRequest request) {
        if (!messageClassifier.isQuestionRequest(content)) {
            return responseAssembler.tip(PracticeConstants.PHASE_QUESTIONING, "这里专注 AI 刷题，你可以输入：请给我出一道 RAG 类型的题，或点击开始刷题。");
        }
        List<String> requestedTypes = questionSelectionService.mergeRequestedTypes(content, request == null ? null : request.questionTypes());
        PracticeQuestionRecord question = questionSelectionService.selectQuestion(userId, requestedTypes);
        practiceSessionService.startAnswering(userId, question);
        return responseAssembler.questionResponse(question, "收到，你的出题请求已处理，请作答当前题目。");
    }

    /**
     * 处理答题阶段消息。
     *
     * @param userId 用户ID
     * @param content 消息内容
     * @return 响应
     */
    private PracticeMessageResponse handleAnsweringMessage(Long userId, String content) {
        if (messageClassifier.isRetryRequest(content) || messageClassifier.isNextRequest(content)) {
            return responseAssembler.tip(PracticeConstants.PHASE_ANSWERING, "请先提交当前题答案；如果想换题，可以点击下一题按钮。");
        }
        PracticeQuestionRecord question = practiceSessionService.currentQuestion(userId);
        if (messageClassifier.isUnrelatedToPractice(content)) {
            return responseAssembler.tip(PracticeConstants.PHASE_ANSWERING, "当前处于答题阶段，请先围绕本题提交你的答案。完成评分后，我再陪你分析本题细节。");
        }
        return practiceGradingService.submitAnswer(userId, question, content);
    }

    /**
     * 流式处理讨论阶段消息。
     *
     * @param userId 用户ID
     * @param content 消息内容
     * @param request 原始请求
     * @param chunkConsumer 文本片段处理器
     * @return 响应
     */
    private PracticeMessageResponse handleDiscussingMessageStream(
            Long userId,
            String content,
            PracticeMessageRequest request,
            Consumer<String> chunkConsumer) {
        if (messageClassifier.isRetryRequest(content)) {
            return buildRetryCurrentQuestionResponse(userId);
        }
        if (messageClassifier.isNextRequest(content) || messageClassifier.isExplicitQuestionRequest(content)) {
            return startRequestedQuestion(userId, content, request);
        }
        PracticeQuestionRecord question = practiceSessionService.currentQuestion(userId);
        if (messageClassifier.isUnrelatedToPractice(content)) {
            return responseAssembler.tip(PracticeConstants.PHASE_DISCUSSING, "当前是本题讨论阶段，请围绕本题的技术概念、解题思路或答案细节提问。");
        }
        return practiceDiscussionService.discussCurrentQuestion(userId, question, content, chunkConsumer);
    }

    /**
     * 按用户请求切换到新题。
     *
     * @param userId 用户ID
     * @param content 消息内容
     * @param request 原始请求
     * @return 新题响应
     */
    private PracticeMessageResponse startRequestedQuestion(Long userId, String content, PracticeMessageRequest request) {
        List<String> requestedTypes = questionSelectionService.mergeRequestedTypes(content, request == null ? null : request.questionTypes());
        PracticeQuestionRecord question = questionSelectionService.selectQuestion(userId, requestedTypes);
        practiceSessionService.startAnswering(userId, question);
        return responseAssembler.questionResponse(question, "已根据你的请求切换到新题，请开始作答。");
    }

    /**
     * 构造重新回答当前题响应。
     *
     * @param userId 用户ID
     * @return 重新回答响应
     */
    private PracticeMessageResponse buildRetryCurrentQuestionResponse(Long userId) {
        PracticeQuestionRecord question = practiceSessionService.currentQuestion(userId);
        practiceSessionService.startAnswering(userId, question);
        return responseAssembler.questionResponse(question, "已重新进入本题作答，请再次提交你的答案。");
    }

    /**
     * 同步流式消息处理后的跨端展示聊天记录。
     *
     * @param userId 用户ID
     * @param content 用户输入
     * @param previousPhase 处理前阶段
     * @param previousQuestionCode 处理前题目编码
     * @param response 处理结果
     */
    private void syncChatHistoryAfterStreamMessage(
            Long userId,
            String content,
            String previousPhase,
            String previousQuestionCode,
            PracticeMessageResponse response) {
        if (PracticeConstants.ACTION_QUESTION.equals(response.action()) && shouldReplaceChatHistory(previousPhase, previousQuestionCode, response)) {
            practiceChatHistoryService.replaceWithAssistantMessage(userId, response);
            return;
        }
        if (!PracticeConstants.PHASE_QUESTIONING.equals(previousPhase)) {
            practiceChatHistoryService.appendConversationTurn(userId, content, response);
        }
    }

    /**
     * 判断是否需要用新题消息覆盖当前轮聊天记录。
     *
     * @param previousPhase 处理前阶段
     * @param previousQuestionCode 处理前题目编码
     * @param response 处理结果
     * @return 是否覆盖
     */
    private boolean shouldReplaceChatHistory(String previousPhase, String previousQuestionCode, PracticeMessageResponse response) {
        if (PracticeConstants.PHASE_QUESTIONING.equals(previousPhase)) {
            return true;
        }
        if (response.question() == null) {
            return false;
        }

        // 切换新题时覆盖旧轮；同题重答走追加，保持原聊天上下文体验。
        return !response.question().code().equals(previousQuestionCode);
    }

    /**
     * 获取当前用户ID。
     *
     * @return 用户ID
     */
    private Long currentUserId() {
        return AuthSupport.requireCurrentUserId();
    }
}
