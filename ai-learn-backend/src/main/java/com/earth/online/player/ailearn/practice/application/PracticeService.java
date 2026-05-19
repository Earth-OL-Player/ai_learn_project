package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.answer.domain.AnswerGradingPort;
import com.earth.online.player.ailearn.answer.domain.GradingResult;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.growth.application.GrowthAwardService;
import com.earth.online.player.ailearn.growth.application.GrowthService;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeAiClient;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeAiGradingResult;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeMapper;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeSessionRecord;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeStatRecord;
import com.earth.online.player.ailearn.practice.interfaces.PracticeActionRequest;
import com.earth.online.player.ailearn.practice.interfaces.PracticeGradingResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeMessageRequest;
import com.earth.online.player.ailearn.practice.interfaces.PracticeMessageResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeQuestionResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeStateResponse;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * AI 智能刷题应用服务。
 */
@Service
public class PracticeService {

    private static final TypeReference<List<DiscussionHistoryMessage>> DISCUSSION_HISTORY_TYPE = new TypeReference<>() {
    };

    private final PracticeMapper practiceMapper;
    private final GrowthMapper growthMapper;
    private final UserMapper userMapper;
    private final GrowthService growthService;
    private final GrowthAwardService growthAwardService;
    private final AnswerGradingPort answerGradingPort;
    private final PracticeAiClient practiceAiClient;
    private final ObjectMapper objectMapper;

    /**
     * 创建 AI 智能刷题服务。
     *
     * @param practiceMapper 刷题仓储
     * @param growthMapper 成长仓储
     * @param userMapper 用户仓储
     * @param growthService 成长服务
     * @param growthAwardService 勋章发放服务
     * @param answerGradingPort 本地评分端口
     * @param practiceAiClient AI 服务客户端
     * @param objectMapper JSON 序列化器
     */
    public PracticeService(
            PracticeMapper practiceMapper,
            GrowthMapper growthMapper,
            UserMapper userMapper,
            GrowthService growthService,
            GrowthAwardService growthAwardService,
            AnswerGradingPort answerGradingPort,
            PracticeAiClient practiceAiClient,
            ObjectMapper objectMapper) {
        this.practiceMapper = practiceMapper;
        this.growthMapper = growthMapper;
        this.userMapper = userMapper;
        this.growthService = growthService;
        this.growthAwardService = growthAwardService;
        this.answerGradingPort = answerGradingPort;
        this.practiceAiClient = practiceAiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询题目分类。
     *
     * @return 分类列表
     */
    public List<String> findQuestionTypes() {
        currentUserId();
        return practiceMapper.findQuestionTypes();
    }

    /**
     * 查询当前刷题状态。
     *
     * @return 当前状态
     */
    public PracticeStateResponse getState() {
        Long userId = currentUserId();
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        PracticeQuestionResponse question = null;
        if (session != null && StringUtils.hasText(session.getQuestionCode())) {
            PracticeQuestionRecord record = practiceMapper.findQuestionByCode(userId, session.getQuestionCode());
            question = record == null ? null : toQuestionResponse(record);
        }
        String phase = session == null ? PracticeConstants.PHASE_QUESTIONING : session.getPhase();
        return new PracticeStateResponse(
                phase,
                phaseText(phase),
                question,
                session == null ? null : session.getLastScore(),
                practiceMapper.findQuestionTypes(),
                growthService.getCurrentGrowth()
        );
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
        PracticeQuestionRecord question = selectQuestion(userId, normalizeTypes(request == null ? null : request.questionTypes()));
        practiceMapper.upsertQuestionSession(userId, question.getCode(), PracticeConstants.PHASE_ANSWERING);
        return questionResponse(question, "已为你抽取一道新题，请认真作答。答完后我会给出评分和建议。");
    }

    /**
     * 重新回答当前题。
     *
     * @return 重新回答响应
     */
    @Transactional
    public PracticeMessageResponse retryCurrentQuestion() {
        Long userId = currentUserId();
        PracticeQuestionRecord question = currentQuestion(userId);
        practiceMapper.upsertQuestionSession(userId, question.getCode(), PracticeConstants.PHASE_ANSWERING);
        return questionResponse(question, "已重新进入本题作答，请再次提交你的答案。");
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
        String content = normalizeContent(request == null ? null : request.content());
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        String phase = session == null ? PracticeConstants.PHASE_QUESTIONING : session.getPhase();

        // 出题和评分仍需要完整业务结果；讨论阶段优先使用真实模型流式输出。
        if (PracticeConstants.PHASE_QUESTIONING.equals(phase)) {
            return handleQuestioningMessage(userId, content, request);
        }
        if (PracticeConstants.PHASE_ANSWERING.equals(phase)) {
            return handleAnsweringMessage(userId, content);
        }
        return handleDiscussingMessageStream(userId, content, request, chunkConsumer);
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
        if (!isQuestionRequest(content)) {
            return tip(PracticeConstants.PHASE_QUESTIONING, "这里专注 AI 刷题，你可以输入：请给我出一道 RAG 类型的题，或点击开始刷题。");
        }
        List<String> requestedTypes = mergeRequestedTypes(content, request == null ? null : request.questionTypes());
        PracticeQuestionRecord question = selectQuestion(userId, requestedTypes);
        practiceMapper.upsertQuestionSession(userId, question.getCode(), PracticeConstants.PHASE_ANSWERING);
        return questionResponse(question, "收到，你的出题请求已处理，请作答当前题目。");
    }

    /**
     * 处理答题阶段消息。
     *
     * @param userId 用户ID
     * @param content 消息内容
     * @return 响应
     */
    private PracticeMessageResponse handleAnsweringMessage(Long userId, String content) {
        if (isRetryRequest(content) || isNextRequest(content)) {
            return tip(PracticeConstants.PHASE_ANSWERING, "请先提交当前题答案；如果想换题，可以点击下一题按钮。");
        }
        PracticeQuestionRecord question = currentQuestion(userId);
        if (isUnrelatedToPractice(content)) {
            return tip(PracticeConstants.PHASE_ANSWERING, "当前处于答题阶段，请先围绕本题提交你的答案。完成评分后，我再陪你分析本题细节。");
        }
        return submitAnswer(userId, question, content);
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
        if (isRetryRequest(content)) {
            return retryCurrentQuestion();
        }
        if (isNextRequest(content) || isExplicitQuestionRequest(content)) {
            List<String> requestedTypes = mergeRequestedTypes(content, request == null ? null : request.questionTypes());
            PracticeQuestionRecord question = selectQuestion(userId, requestedTypes);
            practiceMapper.upsertQuestionSession(userId, question.getCode(), PracticeConstants.PHASE_ANSWERING);
            return questionResponse(question, "已根据你的请求切换到新题，请开始作答。");
        }
        PracticeQuestionRecord question = currentQuestion(userId);
        if (isUnrelatedToPractice(content)) {
            return tip(PracticeConstants.PHASE_DISCUSSING, "当前是本题讨论阶段，请围绕本题的技术概念、解题思路或答案细节提问。");
        }
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        String lastUserAnswer = session == null ? "" : session.getLastAnswerText();
        String gradingSummary = session == null ? "" : session.getLastGradingSummary();
        String historyJson = session == null ? "" : session.getDiscussionHistoryJson();
        String reply = practiceAiClient.discussStream(question, lastUserAnswer, gradingSummary, historyJson, content, chunkConsumer)
                .orElseGet(this::buildLocalDiscussionReply);
        saveDiscussionHistory(userId, historyJson, content, reply);
        return discussionResponse(userId, question, reply);
    }

    /**
     * 提交并评分答案。
     *
     * @param userId 用户ID
     * @param question 当前题目
     * @param userAnswer 用户答案
     * @return 评分响应
     */
    private PracticeMessageResponse submitAnswer(Long userId, PracticeQuestionRecord question, String userAnswer) {
        Optional<PracticeAiGradingResult> aiGradingResult = practiceAiClient.grade(userId, question, userAnswer);
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
        PracticeGradingResponse grading = recordSummaryAndBuildResponse(userId, question, gradingResult, fallbackUsed);
        practiceMapper.updateSessionPhase(
                userId,
                PracticeConstants.PHASE_DISCUSSING,
                grading.score(),
                limitStoredAnswer(userAnswer),
                buildGradingSummary(grading)
        );

        // 评分完成后进入本题讨论阶段，并仅保存当前题最近一次答案用于后续追问上下文。
        String message = buildGradingMessage(fallbackUsed);
        return new PracticeMessageResponse(
                PracticeConstants.ACTION_GRADING,
                PracticeConstants.PHASE_DISCUSSING,
                message,
                toQuestionResponse(question),
                grading,
                growthService.getCurrentGrowth(grading.newBadges())
        );
    }

    /**
     * 记录汇总并构建评分响应。
     *
     * @param userId 用户ID
     * @param question 题目
     * @param gradingResult 评分结果
     * @param fallbackUsed 是否使用兜底评分
     * @return 评分响应
     */
    private PracticeGradingResponse recordSummaryAndBuildResponse(
            Long userId,
            PracticeQuestionRecord question,
            GradingResult gradingResult,
            boolean fallbackUsed) {
        PracticeStatRecord oldStat = practiceMapper.findStat(userId, question.getCode());
        int previousBest = oldStat == null || oldStat.getBestScore() == null ? 0 : oldStat.getBestScore();
        int previousLast = oldStat == null || oldStat.getLastScore() == null ? 0 : oldStat.getLastScore();
        int score = Math.max(0, Math.min(100, gradingResult.score()));
        int earnedExperience = Math.max(0, score - previousBest);
        practiceMapper.upsertStat(userId, question.getCode(), score);

        // 经验值按所有题目最高分总和重算，避免重复答题造成累计偏差。
        int totalExperience = Math.max(0, growthMapper.sumBestScores(userId));
        GrowthLevel level = GrowthLevel.resolveByExperience(totalExperience);
        GrowthRank rank = GrowthRank.resolveByExperience(totalExperience);
        userMapper.updateGrowth(userId, totalExperience, level.code(), rank.code());
        List<BadgeResponse> newBadges = growthAwardService.awardAfterAnswer(userId, score);
        return new PracticeGradingResponse(
                score,
                gradingResult.isCorrect(),
                gradingResult.hitPoints(),
                gradingResult.missingPoints(),
                gradingResult.problems(),
                gradingResult.referenceAnswer(),
                gradingResult.improvementAdvice(),
                gradingResult.reviewKnowledgePoints(),
                earnedExperience,
                previousBest,
                oldStat == null ? null : previousLast,
                buildExperienceDetail(oldStat == null, previousBest, previousLast, score, earnedExperience),
                totalExperience,
                newBadges,
                fallbackUsed
        );
    }

    /**
     * 构造讨论响应并处理追问勋章。
     *
     * @param userId 用户ID
     * @param question 当前题目
     * @param reply 回复内容
     * @return 讨论响应
     */
    private PracticeMessageResponse discussionResponse(Long userId, PracticeQuestionRecord question, String reply) {
        int followUpCount = incrementDiscussionFollowUpCount(userId);
        List<BadgeResponse> newBadges = growthAwardService.awardAfterDiscussion(userId, followUpCount);

        // 追问类勋章通过成长信息透传给前端，评分结果为空时仍可弹框提示。
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
     * 增加并读取当前题连续追问次数。
     *
     * @param userId 用户ID
     * @return 追问次数
     */
    private int incrementDiscussionFollowUpCount(Long userId) {
        practiceMapper.incrementDiscussionFollowUpCount(userId);
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        return session == null ? 0 : safeInt(session.getDiscussionFollowUpCount());
    }

    /**
     * 构造经验变化说明。
     *
     * @param firstAnswer 是否首次答题
     * @param previousBest 评分前历史最高分
     * @param previousLast 评分前最近一次得分
     * @param score 本次得分
     * @param earnedExperience 本次新增经验
     * @return 经验变化说明
     */
    private String buildExperienceDetail(boolean firstAnswer, int previousBest, int previousLast, int score, int earnedExperience) {
        if (firstAnswer) {
            return "首次回答得了 " + score + " 分，新增 " + earnedExperience + " 经验。";
        }
        if (earnedExperience > 0) {
            return "比上次回答多拿了 " + Math.max(0, score - previousLast) + " 分，并突破历史最高分新增 " + earnedExperience + " 经验。";
        }
        return "未能突破上次最高分 " + previousBest + " 分，本次经验不变。";
    }

    /**
     * 构造当前题评分摘要。
     *
     * @param grading 评分响应
     * @return 评分摘要
     */
    private String buildGradingSummary(PracticeGradingResponse grading) {
        StringBuilder builder = new StringBuilder();
        builder.append("本次评分：").append(grading.score()).append("分，")
                .append(Boolean.TRUE.equals(grading.correct()) ? "基本正确" : "仍需改进").append("。");

        // 评分摘要用于后续追问上下文，不需要保留全部前端展示字段。
        appendSummaryList(builder, "命中点", grading.hitPoints());
        appendSummaryList(builder, "缺失点", grading.missingPoints());
        appendSummaryList(builder, "问题点", grading.problems());
        appendSummaryList(builder, "建议复习", grading.reviewKnowledgePoints());
        builder.append("优化建议：").append(grading.improvementAdvice()).append("。");
        builder.append("评分来源：").append(Boolean.TRUE.equals(grading.fallbackUsed()) ? "本地兜底评分" : "AI评分").append("。");
        return limitText(builder.toString(), PracticeConstants.MAX_GRADING_SUMMARY_LENGTH);
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
     * 保存当前题讨论历史。
     *
     * @param userId 用户ID
     * @param historyJson 原始历史JSON
     * @param userMessage 用户问题
     * @param assistantReply AI回复
     */
    private void saveDiscussionHistory(Long userId, String historyJson, String userMessage, String assistantReply) {
        List<DiscussionHistoryMessage> history = new ArrayList<>(readDiscussionHistory(historyJson));
        history.add(new DiscussionHistoryMessage("user", limitText(userMessage, PracticeConstants.MAX_DISCUSSION_HISTORY_CONTENT_LENGTH)));
        history.add(new DiscussionHistoryMessage("assistant", limitText(assistantReply, PracticeConstants.MAX_DISCUSSION_HISTORY_CONTENT_LENGTH)));

        // 只保留最近若干条消息，避免上下文过长影响响应速度。
        int fromIndex = Math.max(0, history.size() - PracticeConstants.MAX_DISCUSSION_HISTORY_MESSAGES);
        List<DiscussionHistoryMessage> limitedHistory = history.subList(fromIndex, history.size());
        practiceMapper.updateDiscussionHistory(userId, writeDiscussionHistory(limitedHistory));
    }

    /**
     * 读取当前题讨论历史。
     *
     * @param historyJson 历史JSON
     * @return 历史消息
     */
    private List<DiscussionHistoryMessage> readDiscussionHistory(String historyJson) {
        if (!StringUtils.hasText(historyJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(historyJson, DISCUSSION_HISTORY_TYPE);
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

    /**
     * 序列化当前题讨论历史。
     *
     * @param history 历史消息
     * @return JSON字符串
     */
    private String writeDiscussionHistory(List<DiscussionHistoryMessage> history) {
        try {
            return objectMapper.writeValueAsString(history);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
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
     * 选择题目。
     *
     * @param userId 用户ID
     * @param questionTypes 题目分类
     * @return 题目记录
     */
    private PracticeQuestionRecord selectQuestion(Long userId, List<String> questionTypes) {
        List<PracticeQuestionRecord> candidates = practiceMapper.findCandidates(userId, questionTypes);
        if (candidates.isEmpty()) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "当前分类下暂无可用题目，请先导入系统题库");
        }
        PracticeQuestionRecord selected = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (PracticeQuestionRecord candidate : candidates) {
            double weightedScore = calculateWeight(candidate) * ThreadLocalRandom.current().nextDouble(0.75D, 1.35D);
            if (weightedScore > bestScore) {
                bestScore = weightedScore;
                selected = candidate;
            }
        }
        return selected == null ? candidates.get(0) : selected;
    }

    /**
     * 计算抽题权重。
     *
     * @param question 候选题目
     * @return 权重
     */
    private double calculateWeight(PracticeQuestionRecord question) {
        int answeredCount = safeInt(question.getAnsweredCount());
        int bestScore = safeInt(question.getBestScore());
        double importanceScore = safeDouble(question.getImportanceScore());
        int occurrenceCount = safeInt(question.getOccurrenceCount());

        // 次数越少、重要性越高、历史得分越低，权重越高。
        double weight = 1.0D;
        weight += (importanceScore / 100.0D) * 4.0D;
        weight += 3.0D / (1 + answeredCount);
        weight += bestScore == 0 ? 2.5D : ((100 - bestScore) / 100.0D) * 3.0D;
        weight += Math.min(1.5D, Math.log10(occurrenceCount + 1.0D) / 2.0D);
        return Math.max(0.1D, weight);
    }

    /**
     * 查询当前题目。
     *
     * @param userId 用户ID
     * @return 当前题目
     */
    private PracticeQuestionRecord currentQuestion(Long userId) {
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        if (session == null || !StringUtils.hasText(session.getQuestionCode())) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "请先开始刷题");
        }
        PracticeQuestionRecord question = practiceMapper.findQuestionByCode(userId, session.getQuestionCode());
        if (question == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "当前题目已下架，请重新出题");
        }
        return question;
    }

    /**
     * 构造出题响应。
     *
     * @param question 题目
     * @param message 提示消息
     * @return 出题响应
     */
    private PracticeMessageResponse questionResponse(PracticeQuestionRecord question, String message) {
        return new PracticeMessageResponse(PracticeConstants.ACTION_QUESTION, PracticeConstants.PHASE_ANSWERING, message, toQuestionResponse(question), null, growthService.getCurrentGrowth());
    }

    /**
     * 构造提示响应。
     *
     * @param phase 当前阶段
     * @param message 提示消息
     * @return 提示响应
     */
    private PracticeMessageResponse tip(String phase, String message) {
        return new PracticeMessageResponse(PracticeConstants.ACTION_TIP, phase, message, null, null, growthService.getCurrentGrowth());
    }

    /**
     * 转换题目响应。
     *
     * @param question 题目记录
     * @return 响应对象
     */
    private PracticeQuestionResponse toQuestionResponse(PracticeQuestionRecord question) {
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
     * 合并用户输入中的分类和显式选择分类。
     *
     * @param content 用户输入
     * @param selectedTypes 已选择分类
     * @return 分类列表
     */
    private List<String> mergeRequestedTypes(String content, List<String> selectedTypes) {
        List<String> availableTypes = practiceMapper.findQuestionTypes();
        List<String> normalizedSelected = normalizeTypes(selectedTypes);
        List<String> mentionedTypes = availableTypes.stream()
                .filter(type -> content.toUpperCase(Locale.ROOT).contains(type.toUpperCase(Locale.ROOT)))
                .map(String::trim)
                .toList();
        return mentionedTypes.isEmpty() ? normalizedSelected : mentionedTypes;
    }

    /**
     * 规整题目分类。
     *
     * @param questionTypes 原始分类
     * @return 分类列表
     */
    private List<String> normalizeTypes(List<String> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return questionTypes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 规整消息内容。
     *
     * @param content 原始内容
     * @return 安全内容
     */
    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "请输入内容");
        }
        return content.trim();
    }

    /**
     * 判断是否为出题请求。
     *
     * @param content 用户输入
     * @return 是否出题
     */
    private boolean isQuestionRequest(String content) {
        return content.contains("出题")
                || content.contains("来一题")
                || content.contains("来一道")
                || content.contains("随机")
                || content.contains("开始")
                || content.contains("刷题")
                || content.contains("题目")
                || content.contains("类型的题");
    }


    /**
     * 判断是否为明确的新出题请求。
     *
     * @param content 用户输入
     * @return 是否明确请求新题
     */
    private boolean isExplicitQuestionRequest(String content) {
        return content.contains("出题")
                || content.contains("来一题")
                || content.contains("来一道")
                || content.contains("随机")
                || content.contains("开始刷题")
                || content.contains("类型的题");
    }

    /**
     * 判断是否为下一题请求。
     *
     * @param content 用户输入
     * @return 是否下一题
     */
    private boolean isNextRequest(String content) {
        return content.contains("下一题") || content.contains("换一题") || content.contains("再来一题");
    }

    /**
     * 判断是否为重新回答请求。
     *
     * @param content 用户输入
     * @return 是否重新回答
     */
    private boolean isRetryRequest(String content) {
        return content.contains("重新回答") || content.contains("再答一次") || content.contains("重答");
    }

    /**
     * 判断用户输入是否偏离刷题上下文。
     *
     * @param content 用户输入
     * @return 是否无关
     */
    private boolean isUnrelatedToPractice(String content) {
        // 当前迭代取消大模型相关性判断，避免额外模型调用拉长用户等待时间。
        return isObviouslyUnrelated(content);
    }

    /**
     * 判断是否明显无关。
     *
     * @param content 用户输入
     * @return 是否无关
     */
    private boolean isObviouslyUnrelated(String content) {
        return PracticeConstants.UNRELATED_WORDS.stream().anyMatch(content::contains);
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
     * 构造本地讨论回复。
     *
     * @return 讨论回复
     */
    private String buildLocalDiscussionReply() {
        return PracticeConstants.FALLBACK_DISCUSSION_MESSAGE;
    }

    /**
     * 获取当前用户ID。
     *
     * @return 用户ID
     */
    private Long currentUserId() {
        return AuthSupport.requireCurrentUserId();
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
     * 获取安全整数。
     *
     * @param value 原始值
     * @return 安全值
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 将可空小数转换为安全值。
     *
     * @param value 原始值
     * @return 安全值
     */
    private double safeDouble(BigDecimal value) {
        return value == null ? 0.0D : value.doubleValue();
    }

    /**
     * 当前题讨论历史消息。
     *
     * @param role 消息角色
     * @param content 消息内容
     */
    private static record DiscussionHistoryMessage(String role, String content) {
    }
}

