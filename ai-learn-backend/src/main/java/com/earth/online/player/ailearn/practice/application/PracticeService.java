package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.answer.domain.AnswerGradingPort;
import com.earth.online.player.ailearn.answer.domain.GradingResult;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.growth.application.GrowthService;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import com.earth.online.player.ailearn.growth.interfaces.GrowthResponse;
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
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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

    private static final String PHASE_QUESTIONING = "QUESTIONING";
    private static final String PHASE_ANSWERING = "ANSWERING";
    private static final String PHASE_DISCUSSING = "DISCUSSING";
    private static final String ACTION_QUESTION = "QUESTION";
    private static final String ACTION_GRADING = "GRADING";
    private static final String ACTION_DISCUSSION = "DISCUSSION";
    private static final String ACTION_TIP = "TIP";
    private static final String FALLBACK_DISCUSSION_MESSAGE = "抱歉，当前大模型调用异常，仅保留兜底策略评分功能，无法和您进行探讨。";
    private static final int MAX_STORED_ANSWER_LENGTH = 4000;
    private static final Set<String> UNRELATED_WORDS = Set.of("天气", "新闻", "股票", "旅游", "做饭", "写诗", "翻译", "笑话", "帅", "好看");

    private final PracticeMapper practiceMapper;
    private final GrowthMapper growthMapper;
    private final UserMapper userMapper;
    private final GrowthService growthService;
    private final AnswerGradingPort answerGradingPort;
    private final PracticeAiClient practiceAiClient;

    /**
     * 创建 AI 智能刷题服务。
     *
     * @param practiceMapper 刷题仓储
     * @param growthMapper 成长仓储
     * @param userMapper 用户仓储
     * @param growthService 成长服务
     * @param answerGradingPort 本地评分端口
     * @param practiceAiClient AI 服务客户端
     */
    public PracticeService(
            PracticeMapper practiceMapper,
            GrowthMapper growthMapper,
            UserMapper userMapper,
            GrowthService growthService,
            AnswerGradingPort answerGradingPort,
            PracticeAiClient practiceAiClient) {
        this.practiceMapper = practiceMapper;
        this.growthMapper = growthMapper;
        this.userMapper = userMapper;
        this.growthService = growthService;
        this.answerGradingPort = answerGradingPort;
        this.practiceAiClient = practiceAiClient;
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
        String phase = session == null ? PHASE_QUESTIONING : session.getPhase();
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
        practiceMapper.upsertQuestionSession(userId, question.getCode(), PHASE_ANSWERING);
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
        practiceMapper.upsertQuestionSession(userId, question.getCode(), PHASE_ANSWERING);
        return questionResponse(question, "已重新进入本题作答，请再次提交你的答案。");
    }

    /**
     * 处理聊天消息。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @Transactional
    public PracticeMessageResponse handleMessage(PracticeMessageRequest request) {
        Long userId = currentUserId();
        String content = normalizeContent(request == null ? null : request.content());
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        String phase = session == null ? PHASE_QUESTIONING : session.getPhase();

        // 出题阶段只允许出题相关表达，防止刷题入口被当作通用聊天使用。
        if (PHASE_QUESTIONING.equals(phase)) {
            return handleQuestioningMessage(userId, content, request);
        }
        if (PHASE_ANSWERING.equals(phase)) {
            return handleAnsweringMessage(userId, content);
        }
        return handleDiscussingMessage(userId, content, request);
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
        String phase = session == null ? PHASE_QUESTIONING : session.getPhase();

        // 出题和评分仍需要完整业务结果；讨论阶段优先使用真实模型流式输出。
        if (PHASE_QUESTIONING.equals(phase)) {
            return handleQuestioningMessage(userId, content, request);
        }
        if (PHASE_ANSWERING.equals(phase)) {
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
            return tip(PHASE_QUESTIONING, "这里专注 AI 刷题，你可以输入：请给我出一道 RAG 类型的题，或点击开始刷题。");
        }
        List<String> requestedTypes = mergeRequestedTypes(content, request == null ? null : request.questionTypes());
        PracticeQuestionRecord question = selectQuestion(userId, requestedTypes);
        practiceMapper.upsertQuestionSession(userId, question.getCode(), PHASE_ANSWERING);
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
            return tip(PHASE_ANSWERING, "请先提交当前题答案；如果想换题，可以点击下一题按钮。");
        }
        PracticeQuestionRecord question = currentQuestion(userId);
        if (isUnrelatedToPractice(question, PHASE_ANSWERING, content)) {
            return tip(PHASE_ANSWERING, "当前处于答题阶段，请先围绕本题提交你的答案。完成评分后，我再陪你分析本题细节。");
        }
        return submitAnswer(userId, question, content);
    }

    /**
     * 处理讨论阶段消息。
     *
     * @param userId 用户ID
     * @param content 消息内容
     * @param request 原始请求
     * @return 响应
     */
    private PracticeMessageResponse handleDiscussingMessage(Long userId, String content, PracticeMessageRequest request) {
        if (isRetryRequest(content)) {
            return retryCurrentQuestion();
        }
        if (isNextRequest(content) || isExplicitQuestionRequest(content)) {
            List<String> requestedTypes = mergeRequestedTypes(content, request == null ? null : request.questionTypes());
            PracticeQuestionRecord question = selectQuestion(userId, requestedTypes);
            practiceMapper.upsertQuestionSession(userId, question.getCode(), PHASE_ANSWERING);
            return questionResponse(question, "已根据你的请求切换到新题，请开始作答。");
        }
        PracticeQuestionRecord question = currentQuestion(userId);
        if (isUnrelatedToPractice(question, PHASE_DISCUSSING, content)) {
            return tip(PHASE_DISCUSSING, "当前是本题讨论阶段，请围绕本题的技术概念、解题思路或答案细节提问。");
        }
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        String lastUserAnswer = session == null ? "" : session.getLastAnswerText();
        String reply = practiceAiClient.discuss(question, lastUserAnswer, content).orElseGet(this::buildLocalDiscussionReply);
        return new PracticeMessageResponse(ACTION_DISCUSSION, PHASE_DISCUSSING, reply, toQuestionResponse(question), null, growthService.getCurrentGrowth());
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
            practiceMapper.upsertQuestionSession(userId, question.getCode(), PHASE_ANSWERING);
            return questionResponse(question, "已根据你的请求切换到新题，请开始作答。");
        }
        PracticeQuestionRecord question = currentQuestion(userId);
        if (isUnrelatedToPractice(question, PHASE_DISCUSSING, content)) {
            return tip(PHASE_DISCUSSING, "当前是本题讨论阶段，请围绕本题的技术概念、解题思路或答案细节提问。");
        }
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        String lastUserAnswer = session == null ? "" : session.getLastAnswerText();
        String reply = practiceAiClient.discussStream(question, lastUserAnswer, content, chunkConsumer)
                .orElseGet(this::buildLocalDiscussionReply);
        return new PracticeMessageResponse(ACTION_DISCUSSION, PHASE_DISCUSSING, reply, toQuestionResponse(question), null, growthService.getCurrentGrowth());
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
        practiceMapper.updateSessionPhase(userId, PHASE_DISCUSSING, grading.score(), limitStoredAnswer(userAnswer));

        // 评分完成后进入本题讨论阶段，并仅保存当前题最近一次答案用于后续追问上下文。
        String message = buildGradingMessage(fallbackUsed);
        return new PracticeMessageResponse(ACTION_GRADING, PHASE_DISCUSSING, message, toQuestionResponse(question), grading, growthService.getCurrentGrowth());
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
                Collections.emptyList(),
                fallbackUsed
        );
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
        return new PracticeMessageResponse(ACTION_QUESTION, PHASE_ANSWERING, message, toQuestionResponse(question), null, growthService.getCurrentGrowth());
    }

    /**
     * 构造提示响应。
     *
     * @param phase 当前阶段
     * @param message 提示消息
     * @return 提示响应
     */
    private PracticeMessageResponse tip(String phase, String message) {
        return new PracticeMessageResponse(ACTION_TIP, phase, message, null, null, growthService.getCurrentGrowth());
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
     * @param question 当前题目
     * @param phase 当前阶段
     * @param content 用户输入
     * @return 是否无关
     */
    private boolean isUnrelatedToPractice(PracticeQuestionRecord question, String phase, String content) {
        Optional<Boolean> aiRelevance = practiceAiClient.judgeRelevance(question, phase, content);
        if (aiRelevance.isPresent()) {
            return !aiRelevance.get();
        }

        // 大模型不可用时保留关键词兜底，避免明显闲聊进入评分或讨论。
        return isObviouslyUnrelated(content);
    }

    /**
     * 判断是否明显无关。
     *
     * @param content 用户输入
     * @return 是否无关
     */
    private boolean isObviouslyUnrelated(String content) {
        return UNRELATED_WORDS.stream().anyMatch(content::contains);
    }

    /**
     * 限制当前题答案记忆长度。
     *
     * @param userAnswer 用户答案
     * @return 可保存答案
     */
    private String limitStoredAnswer(String userAnswer) {
        if (userAnswer.length() <= MAX_STORED_ANSWER_LENGTH) {
            return userAnswer;
        }

        // 仅保留足够追问的上下文，避免异常长答案撑大当前会话记录。
        return userAnswer.substring(0, MAX_STORED_ANSWER_LENGTH);
    }

    /**
     * 构造本地讨论回复。
     *
     * @return 讨论回复
     */
    private String buildLocalDiscussionReply() {
        return FALLBACK_DISCUSSION_MESSAGE;
    }

    /**
     * 获取当前用户ID。
     *
     * @return 用户ID
     */
    private Long currentUserId() {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }
        return authenticatedUser.userId();
    }

    /**
     * 转换阶段文案。
     *
     * @param phase 阶段编码
     * @return 阶段文案
     */
    private String phaseText(String phase) {
        if (PHASE_ANSWERING.equals(phase)) {
            return "答题中";
        }
        if (PHASE_DISCUSSING.equals(phase)) {
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
}
