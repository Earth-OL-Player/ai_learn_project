package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeMapper;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeSessionRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 刷题会话状态服务。
 */
@Service
public class PracticeSessionService {

    private final PracticeMapper practiceMapper;

    /**
     * 创建刷题会话状态服务。
     *
     * @param practiceMapper 刷题仓储
     */
    public PracticeSessionService(PracticeMapper practiceMapper) {
        this.practiceMapper = practiceMapper;
    }

    /**
     * 查询当前刷题会话。
     *
     * @param userId 用户ID
     * @return 刷题会话
     */
    public PracticeSessionRecord findSession(Long userId) {
        return practiceMapper.findSession(userId);
    }

    /**
     * 查询会话中的当前题。
     *
     * @param userId 用户ID
     * @param session 当前会话
     * @return 当前题目
     */
    public PracticeQuestionRecord findSessionQuestion(Long userId, PracticeSessionRecord session) {
        if (session == null || !StringUtils.hasText(session.getQuestionCode())) {
            return null;
        }

        // 状态查询允许当前题为空，避免下架题目导致页面状态接口失败。
        return practiceMapper.findQuestionByCode(userId, session.getQuestionCode());
    }

    /**
     * 查询当前题目。
     *
     * @param userId 用户ID
     * @return 当前题目
     */
    public PracticeQuestionRecord currentQuestion(Long userId) {
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        if (session == null || !StringUtils.hasText(session.getQuestionCode())) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "请先开始刷题");
        }

        // 作答和讨论必须依赖有效题目，下架时提示用户重新出题。
        PracticeQuestionRecord question = practiceMapper.findQuestionByCode(userId, session.getQuestionCode());
        if (question == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "当前题目已下架，请重新出题");
        }
        return question;
    }

    /**
     * 进入当前题答题阶段。
     *
     * @param userId 用户ID
     * @param question 题目
     */
    public void startAnswering(Long userId, PracticeQuestionRecord question) {
        practiceMapper.upsertQuestionSession(userId, question.getCode(), PracticeConstants.PHASE_ANSWERING);
    }

    /**
     * 进入当前题讨论阶段。
     *
     * @param userId 用户ID
     * @param score 最近得分
     * @param answerText 最近答案
     * @param gradingSummary 评分摘要
     */
    public void startDiscussing(Long userId, Integer score, String answerText, String gradingSummary) {
        practiceMapper.updateSessionPhase(
                userId,
                PracticeConstants.PHASE_DISCUSSING,
                score,
                answerText,
                gradingSummary
        );
    }

    /**
     * 增加并读取当前题连续追问次数。
     *
     * @param userId 用户ID
     * @return 追问次数
     */
    public int incrementDiscussionFollowUpCount(Long userId) {
        practiceMapper.incrementDiscussionFollowUpCount(userId);
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        return session == null ? 0 : safeInt(session.getDiscussionFollowUpCount());
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
