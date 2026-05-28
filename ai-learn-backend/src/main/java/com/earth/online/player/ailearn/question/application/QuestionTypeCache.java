package com.earth.online.player.ailearn.question.application;

import com.earth.online.player.ailearn.common.cache.CacheInvalidationSupport;
import com.earth.online.player.ailearn.common.cache.LocalTtlCache;
import com.earth.online.player.ailearn.question.infrastructure.QuestionMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 题目分类本地短 TTL 缓存。
 */
@Service
public class QuestionTypeCache {

    private static final String QUESTION_TYPES_KEY = "QUESTION_TYPES";
    private static final Duration QUESTION_TYPE_TTL = Duration.ofMinutes(5);

    private final QuestionMapper questionMapper;
    private final LocalTtlCache<String, List<String>> questionTypesCache = new LocalTtlCache<>(QUESTION_TYPE_TTL);

    /**
     * 创建题目分类缓存服务。
     *
     * @param questionMapper 题库仓储
     */
    public QuestionTypeCache(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    /**
     * 查询题目分类列表。
     *
     * @return 题目分类列表
     */
    public List<String> findQuestionTypes() {
        List<String> cachedTypes = questionTypesCache.get(
                QUESTION_TYPES_KEY,
                () -> List.copyOf(questionMapper.findQuestionTypes())
        );
        return List.copyOf(cachedTypes);
    }

    /**
     * 在事务提交后清理题目分类缓存。
     */
    public void invalidateAfterCommit() {
        CacheInvalidationSupport.afterCommit(questionTypesCache::clear);
    }
}
