package com.earth.online.player.ailearn.analysis.application;

import com.earth.online.player.ailearn.analysis.infrastructure.KnowledgeWeakPointRecord;
import com.earth.online.player.ailearn.analysis.infrastructure.LearningAnalysisMapper;
import com.earth.online.player.ailearn.analysis.interfaces.LearningAnalysisResponse;
import com.earth.online.player.ailearn.analysis.interfaces.WeakPointResponse;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 学习分析应用服务。
 */
@Service
public class LearningAnalysisService {

    private final LearningAnalysisMapper learningAnalysisMapper;

    /**
     * 创建学习分析服务。
     *
     * @param learningAnalysisMapper 学习分析仓储
     */
    public LearningAnalysisService(LearningAnalysisMapper learningAnalysisMapper) {
        this.learningAnalysisMapper = learningAnalysisMapper;
    }

    /**
     * 查询当前用户学习分析。
     *
     * @return 学习分析
     */
    public LearningAnalysisResponse getMine() {
        Long userId = currentUserId();
        List<WeakPointResponse> weakPoints = learningAnalysisMapper.findWeakPoints(userId).stream()
                .map(this::toResponse)
                .toList();
        return new LearningAnalysisResponse(weakPoints);
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
     * 转换薄弱点响应。
     *
     * @param record 查询投影
     * @return 薄弱点响应
     */
    private WeakPointResponse toResponse(KnowledgeWeakPointRecord record) {
        double averageScore = record.getAverageScore() == null ? 0 : record.getAverageScore();
        return new WeakPointResponse(
                String.valueOf(record.getKnowledgePointId()),
                record.getKnowledgePointName(),
                record.getAnsweredCount() == null ? 0 : record.getAnsweredCount(),
                averageScore,
                record.getLowScoreCount() == null ? 0 : record.getLowScoreCount(),
                record.getRecommendedQuestionId() == null ? null : String.valueOf(record.getRecommendedQuestionId()),
                record.getRecommendedQuestionTitle(),
                buildAdvice(record.getKnowledgePointName(), averageScore)
        );
    }

    /**
     * 构建学习建议。
     *
     * @param name 知识点名称
     * @param averageScore 平均分
     * @return 建议文案
     */
    private String buildAdvice(String name, double averageScore) {
        if (averageScore < 60) {
            return "建议优先复习「" + name + "」，先回看参考答案，再重新刷低分题。";
        }
        if (averageScore < 80) {
            return "「" + name + "」仍有提升空间，建议结合场景题补充工程化细节。";
        }
        return "「" + name + "」掌握较好，可定期复盘保持熟练度。";
    }
}
