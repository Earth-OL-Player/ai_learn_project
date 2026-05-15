package com.earth.online.player.ailearn.agent.application;

import com.earth.online.player.ailearn.agent.infrastructure.AgentSessionMapper;
import com.earth.online.player.ailearn.agent.infrastructure.AgentSessionRecord;
import com.earth.online.player.ailearn.agent.infrastructure.AiGradingClient;
import com.earth.online.player.ailearn.agent.infrastructure.PracticeQuestionMapper;
import com.earth.online.player.ailearn.agent.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.agent.interfaces.PracticeQuestionResponse;
import com.earth.online.player.ailearn.agent.interfaces.PracticeSubmitResponse;
import com.earth.online.player.ailearn.agent.interfaces.StartPracticeRequest;
import com.earth.online.player.ailearn.agent.interfaces.SubmitPracticeRequest;
import com.earth.online.player.ailearn.answer.domain.AnswerGradingDomainService;
import com.earth.online.player.ailearn.answer.domain.GradingResult;
import com.earth.online.player.ailearn.answer.infrastructure.AnswerRecordEntity;
import com.earth.online.player.ailearn.answer.infrastructure.AnswerRecordMapper;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.growth.application.GrowthRuleService;
import com.earth.online.player.ailearn.growth.application.GrowthAwardService;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import com.earth.online.player.ailearn.growth.interfaces.GrowthResponse;
import com.earth.online.player.ailearn.question.domain.QuestionDifficulty;
import com.earth.online.player.ailearn.question.domain.QuestionType;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * AI智能刷题应用服务。
 */
@Service
public class PracticeService {

    private static final String SESSION_STARTED = "STARTED";
    private static final int MAX_ANSWER_LENGTH = 5000;
    private static final String SOURCE_SCOPE_DEFAULT = "DEFAULT";
    private static final String SOURCE_SCOPE_MINE = "MINE";
    private static final String SOURCE_SCOPE_MIXED = "MIXED";
    private static final String GRADING_SOURCE_AI_SERVICE = "AI_SERVICE";
    private static final String GRADING_SOURCE_LOCAL_RULE = "LOCAL_RULE";

    private final PracticeQuestionMapper practiceQuestionMapper;
    private final AgentSessionMapper agentSessionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final AnswerGradingDomainService gradingDomainService;
    private final AiGradingClient aiGradingClient;
    private final GrowthRuleService growthRuleService;
    private final GrowthAwardService growthAwardService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建刷题应用服务。
     *
     * @param practiceQuestionMapper 刷题题目仓储
     * @param agentSessionMapper 会话仓储
     * @param answerRecordMapper 答题记录仓储
     * @param gradingDomainService 本地评分领域服务
     * @param growthRuleService 成长规则服务
     * @param userMapper 用户仓储
     * @param objectMapper JSON 序列化器
     */
    public PracticeService(
            PracticeQuestionMapper practiceQuestionMapper,
            AgentSessionMapper agentSessionMapper,
            AnswerRecordMapper answerRecordMapper,
            AnswerGradingDomainService gradingDomainService,
            AiGradingClient aiGradingClient,
            GrowthRuleService growthRuleService,
            GrowthAwardService growthAwardService,
            UserMapper userMapper,
            ObjectMapper objectMapper) {
        this.practiceQuestionMapper = practiceQuestionMapper;
        this.agentSessionMapper = agentSessionMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.gradingDomainService = gradingDomainService;
        this.aiGradingClient = aiGradingClient;
        this.growthRuleService = growthRuleService;
        this.growthAwardService = growthAwardService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 开始一次刷题并创建会话。
     *
     * @param request 开始刷题请求
     * @return 推荐题目
     */
    @Transactional
    public PracticeQuestionResponse start(StartPracticeRequest request) {
        Long userId = currentUserId();
        String difficulty = normalizeDifficulty(request == null ? null : request.difficulty());
        String questionType = normalizeQuestionType(request == null ? null : request.questionType());
        List<Long> knowledgePointIds = parseIds(request == null ? null : request.knowledgePointIds(), "知识点ID不合法");
        String sourceScope = normalizeSourceScope(request == null ? null : request.sourceScope());

        // 本期只从默认题库中选择题目，优先未刷题和低分题。
        PracticeQuestionRecord question = practiceQuestionMapper.findRecommended(
                userId, difficulty, questionType, knowledgePointIds, sourceScope);
        if (question == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "暂无符合条件的题目，请调整筛选条件");
        }

        AgentSessionRecord session = new AgentSessionRecord();
        session.setUserId(userId);
        session.setQuestionId(question.getId());
        session.setStatus(SESSION_STARTED);
        agentSessionMapper.insert(session);
        return toQuestionResponse(session.getId(), question, buildRecommendReason(question));
    }

    /**
     * 提交答案并返回评分结果。
     *
     * @param request 提交请求
     * @return 评分结果
     */
    @Transactional
    public PracticeSubmitResponse submit(SubmitPracticeRequest request) {
        Long userId = currentUserId();
        Long sessionId = parseId(request.sessionId(), "会话ID不合法");
        Long questionId = parseId(request.questionId(), "题目ID不合法");
        String userAnswer = normalizeAnswer(request.userAnswer());

        AgentSessionRecord session = agentSessionMapper.findById(sessionId);
        validateSession(session, userId, questionId);
        PracticeQuestionRecord question = practiceQuestionMapper.findById(questionId, userId);
        if (question == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "题目不存在或已下架");
        }

        List<String> knowledgePoints = splitNames(question.getKnowledgePointNames());
        GradingWithSource gradingWithSource = gradeAnswer(userId, question, knowledgePoints, userAnswer);
        GradingResult gradingResult = gradingWithSource.result();
        boolean firstAttempt = answerRecordMapper.countByUserAndQuestion(userId, questionId) == 0;
        Integer previousMinScore = answerRecordMapper.minScoreByUserAndQuestion(userId, questionId);
        AnswerRecordEntity answerRecord = buildAnswerRecord(
                userId, sessionId, questionId, userAnswer, request.durationSeconds(),
                gradingResult, gradingWithSource.source(), firstAttempt);
        answerRecordMapper.insert(answerRecord);
        agentSessionMapper.markSubmitted(sessionId);

        GrowthResponse growth = updateGrowth(userId, gradingResult.score(), previousMinScore);
        return toSubmitResponse(answerRecord.getId(), gradingResult, gradingWithSource.source(), growth);
    }

    /**
     * 获取当前认证用户ID。
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
     * 校验会话归属和状态。
     *
     * @param session 会话记录
     * @param userId 当前用户ID
     * @param questionId 题目ID
     */
    private void validateSession(AgentSessionRecord session, Long userId, Long questionId) {
        if (session == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "刷题会话不存在");
        }
        if (!userId.equals(session.getUserId()) || !questionId.equals(session.getQuestionId())) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "不能提交不属于自己的刷题会话");
        }
        if (!SESSION_STARTED.equals(session.getStatus())) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "该刷题会话已提交，请重新开始刷题");
        }
    }

    /**
     * 构造答题记录。
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param questionId 题目ID
     * @param userAnswer 用户答案
     * @param durationSeconds 耗时秒数
     * @param gradingResult 评分结果
     * @param firstAttempt 是否首次作答
     * @return 答题记录
     */
    private AnswerRecordEntity buildAnswerRecord(
            Long userId,
            Long sessionId,
            Long questionId,
            String userAnswer,
            Integer durationSeconds,
            GradingResult gradingResult,
            String gradingSource,
            boolean firstAttempt) {
        AnswerRecordEntity record = new AnswerRecordEntity();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setQuestionId(questionId);
        record.setUserAnswer(userAnswer);
        record.setScore(gradingResult.score());
        record.setCorrect(gradingResult.isCorrect());
        record.setAiFeedback(toFeedbackJson(gradingResult));
        record.setGradingSource(gradingSource);
        record.setImprovementAdvice(gradingResult.improvementAdvice());
        record.setDurationSeconds(durationSeconds);
        record.setFirstAttempt(firstAttempt);
        return record;
    }

    /**
     * 更新用户经验、等级和段位。
     *
     * @param userId 用户ID
     * @param score 本次得分
     * @return 成长反馈
     */
    private GrowthResponse updateGrowth(Long userId, int score, Integer previousMinScore) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录状态已失效，请重新登录");
        }
        int earnedExperience = growthRuleService.calculateEarnedExperience(score);
        int currentExperience = Math.max(0, user.getExperience() == null ? 0 : user.getExperience()) + earnedExperience;
        GrowthLevel level = growthRuleService.resolveLevel(currentExperience);
        GrowthRank rank = growthRuleService.resolveRank(currentExperience);

        // 成长字段落在用户表，便于个人中心直接展示最新状态。
        userMapper.updateGrowth(userId, currentExperience, level.code(), rank.code());
        long answeredCount = answerRecordMapper.countByUser(userId);
        double averageScore = answerRecordMapper.averageScoreByUser(userId);
        boolean improved = previousMinScore != null && previousMinScore < 60 && score - previousMinScore >= 20;
        List<BadgeResponse> newBadges = growthAwardService.awardAfterAnswer(
                userId, score, earnedExperience, answeredCount, improved);
        int nextLevelExperience = GrowthLevel.nextLevelExperience(currentExperience);
        return new GrowthResponse(
                earnedExperience,
                currentExperience,
                level.displayCode(),
                level.displayName(),
                rank.displayName(),
                answeredCount,
                averageScore,
                nextLevelExperience,
                Math.max(0, nextLevelExperience - currentExperience),
                growthAwardService.calculateStreakDays(userId),
                growthAwardService.findBadgeWall(userId),
                newBadges,
                growthAwardService.findRecentEvents(userId)
        );
    }

    /**
     * 转换刷题题目响应。
     *
     * @param sessionId 会话ID
     * @param question 题目记录
     * @return 题目响应
     */
    private PracticeQuestionResponse toQuestionResponse(Long sessionId, PracticeQuestionRecord question, String recommendReason) {
        QuestionType questionType = QuestionType.valueOf(question.getQuestionType());
        QuestionDifficulty difficulty = QuestionDifficulty.valueOf(question.getDifficulty());
        return new PracticeQuestionResponse(
                String.valueOf(sessionId),
                String.valueOf(question.getId()),
                question.getTitle(),
                question.getContent(),
                questionType.name(),
                questionType.text(),
                difficulty.name(),
                difficulty.text(),
                splitNames(question.getKnowledgePointNames()),
                question.getSourceType(),
                recommendReason
        );
    }

    /**
     * 转换提交响应。
     *
     * @param answerRecordId 答题记录ID
     * @param result 评分结果
     * @param growth 成长反馈
     * @return 提交响应
     */
    private PracticeSubmitResponse toSubmitResponse(
            Long answerRecordId,
            GradingResult result,
            String gradingSource,
            GrowthResponse growth) {
        return new PracticeSubmitResponse(
                String.valueOf(answerRecordId),
                result.score(),
                result.isCorrect(),
                result.hitPoints(),
                result.missingPoints(),
                result.problems(),
                result.referenceAnswer(),
                result.improvementAdvice(),
                result.reviewKnowledgePoints(),
                gradingSource,
                growth
        );
    }

    /**
     * 优先调用 AI 服务评分，失败时降级本地规则评分。
     *
     * @param userId 用户ID
     * @param question 题目记录
     * @param knowledgePoints 知识点
     * @param userAnswer 用户答案
     * @return 评分结果和来源
     */
    private GradingWithSource gradeAnswer(
            Long userId,
            PracticeQuestionRecord question,
            List<String> knowledgePoints,
            String userAnswer) {
        AiGradingClient.AiGradeRequest request = new AiGradingClient.AiGradeRequest(
                String.valueOf(userId),
                String.valueOf(question.getId()),
                question.getContent(),
                question.getStandardAnswer(),
                userAnswer,
                knowledgePoints,
                Collections.emptyList()
        );
        return aiGradingClient.grade(request)
                .map(result -> new GradingWithSource(result, GRADING_SOURCE_AI_SERVICE))
                .orElseGet(() -> new GradingWithSource(
                        gradingDomainService.grade(question.getStandardAnswer(), knowledgePoints, userAnswer),
                        GRADING_SOURCE_LOCAL_RULE
                ));
    }

    /**
     * 构建推荐原因。
     *
     * @param question 推荐题目
     * @return 推荐原因
     */
    private String buildRecommendReason(PracticeQuestionRecord question) {
        if (question.getOwnerUserId() != null) {
            return "优先从你的个人题库中选择，并结合未刷题、低分题和近期重复情况排序。";
        }
        return "优先从平台默认题库中选择未刷过或低分待复习的题目。";
    }

    /**
     * 序列化评分反馈。
     *
     * @param gradingResult 评分结果
     * @return JSON 字符串
     */
    private String toFeedbackJson(GradingResult gradingResult) {
        try {
            return objectMapper.writeValueAsString(gradingResult);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResponseCode.SYSTEM_ERROR.code(), "评分结果保存失败");
        }
    }

    /**
     * 规整用户答案。
     *
     * @param answer 原始答案
     * @return 安全答案
     */
    private String normalizeAnswer(String answer) {
        String safeAnswer = answer == null ? "" : answer.trim();
        if (!StringUtils.hasText(safeAnswer)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "答案不能为空");
        }
        if (safeAnswer.length() > MAX_ANSWER_LENGTH) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "答案长度不能超过5000个字符");
        }
        return safeAnswer;
    }

    /**
     * 规整难度。
     *
     * @param difficulty 原始难度
     * @return 难度编码
     */
    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return null;
        }
        try {
            return QuestionDifficulty.valueOf(difficulty.trim()).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目难度不合法");
        }
    }

    /**
     * 规整题型。
     *
     * @param questionType 原始题型
     * @return 题型编码
     */
    private String normalizeQuestionType(String questionType) {
        if (!StringUtils.hasText(questionType)) {
            return null;
        }
        try {
            return QuestionType.valueOf(questionType.trim()).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题型不合法");
        }
    }

    /**
     * 规整题库范围。
     *
     * @param sourceScope 原始范围
     * @return 安全范围
     */
    private String normalizeSourceScope(String sourceScope) {
        if (!StringUtils.hasText(sourceScope)) {
            return SOURCE_SCOPE_DEFAULT;
        }
        String safeScope = sourceScope.trim().toUpperCase(java.util.Locale.ROOT);
        if (!SOURCE_SCOPE_DEFAULT.equals(safeScope)
                && !SOURCE_SCOPE_MINE.equals(safeScope)
                && !SOURCE_SCOPE_MIXED.equals(safeScope)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题库范围不合法");
        }
        return safeScope;
    }

    /**
     * 解析字符串ID。
     *
     * @param value 原始ID
     * @param message 错误消息
     * @return 数字ID
     */
    private Long parseId(String value, String message) {
        try {
            long id = Long.parseLong(value);
            if (id < 1) {
                throw new NumberFormatException(message);
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), message);
        }
    }

    /**
     * 解析字符串ID列表。
     *
     * @param values 原始ID列表
     * @param message 错误消息
     * @return 数字ID列表
     */
    private List<Long> parseIds(List<String> values, String message) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> parseId(value, message))
                .toList();
    }

    /**
     * 拆分知识点名称。
     *
     * @param names 聚合名称
     * @return 名称列表
     */
    private List<String> splitNames(String names) {
        if (!StringUtils.hasText(names)) {
            return Collections.emptyList();
        }
        return Arrays.stream(names.split(","))
                .filter(StringUtils::hasText)
                .toList();
    }

    /** 评分结果和来源。 */
    private record GradingWithSource(GradingResult result, String source) {
    }
}
