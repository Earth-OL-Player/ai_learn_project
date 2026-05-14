package com.earth.online.player.ailearn.question.application;

import com.earth.online.player.ailearn.question.infrastructure.KnowledgePointMapper;
import com.earth.online.player.ailearn.question.interfaces.KnowledgePointResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 知识点应用服务。
 */
@Service
public class KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;

    /**
     * 创建知识点服务。
     *
     * @param knowledgePointMapper 知识点仓储
     */
    public KnowledgePointService(KnowledgePointMapper knowledgePointMapper) {
        this.knowledgePointMapper = knowledgePointMapper;
    }

    /**
     * 查询全部有效知识点。
     *
     * @return 知识点列表
     */
    public List<KnowledgePointResponse> findAll() {
        return knowledgePointMapper.findAllActive().stream()
                .map(record -> new KnowledgePointResponse(
                        String.valueOf(record.getId()),
                        record.getName(),
                        record.getDescription()
                ))
                .toList();
    }
}
