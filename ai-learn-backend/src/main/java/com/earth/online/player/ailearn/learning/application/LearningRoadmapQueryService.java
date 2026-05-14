package com.earth.online.player.ailearn.learning.application;

import com.earth.online.player.ailearn.learning.dto.LearningRoadmapResponse;
import com.earth.online.player.ailearn.learning.dto.ResourceItem;
import com.earth.online.player.ailearn.learning.dto.RoadmapSection;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 学习路线查询服务。
 */
@Service
public class LearningRoadmapQueryService {

    /**
     * 查询 AI 应用开发学习路线。
     *
     * @return 学习路线响应
     */
    public LearningRoadmapResponse queryRoadmap() {
        return new LearningRoadmapResponse(
                "AI 应用开发学习路线和资料集",
                "面向 AI 应用开发学习者的阶段化路线，帮助初学者少走弯路。",
                "本平台用于整理 AI 应用开发学习路线、资料与实践方向，本期先提供公开学习路线展示。",
                "路线按基础、进阶、工程、实战四个阶段推进，先打牢编程与机器学习基础，再进入大模型应用工程和 Agent 实战。",
                buildSections(),
                buildResources(),
                buildSuggestions()
        );
    }

    /**
     * 构建学习阶段列表。
     *
     * @return 学习阶段列表
     */
    private List<RoadmapSection> buildSections() {
        return List.of(
                new RoadmapSection("基础阶段", "打好编程、数学和机器学习基础，为后续 AI 应用开发建立共同语言。",
                        List.of("Python 基础", "数学基础", "机器学习基础")),
                new RoadmapSection("进阶阶段", "理解深度学习与大模型关键概念，建立对常见 AI 方向的整体认知。",
                        List.of("深度学习", "NLP 自然语言处理", "CV 计算机视觉", "大模型基础")),
                new RoadmapSection("工程阶段", "掌握 AI 应用开发常用框架和工程化技术，把模型能力接入真实业务流程。",
                        List.of("LangChain", "LangGraph", "RAG 检索增强生成", "向量数据库", "模型部署")),
                new RoadmapSection("实战阶段", "通过项目实践沉淀从需求到落地的完整能力，形成可演示作品。",
                        List.of("AI Agent", "知识库问答", "智能刷题", "企业应用"))
        );
    }

    /**
     * 构建学习资料列表。
     *
     * @return 学习资料列表
     */
    private List<ResourceItem> buildResources() {
        return List.of(
                new ResourceItem("Python-100-Days", "适合系统学习 Python 基础的开源教程。", "https://github.com/jackfrued/Python-100-Days"),
                new ResourceItem("LangChain 官方文档", "学习 AI 应用编排和工具调用的核心资料。", "https://docs.langchain.com"),
                new ResourceItem("all-in-rag", "系统学习 RAG 理论与实践的开源资料。", "https://github.com/datawhalechina/all-in-rag"),
                new ResourceItem("Qdrant", "适合中小型项目使用的向量数据库参考实现。", "https://github.com/qdrant/qdrant"),
                new ResourceItem("RAGFlow", "开箱即用的企业级 RAG 知识库与智能问答平台。", "https://github.com/infiniflow/ragflow")
        );
    }

    /**
     * 构建学习建议列表。
     *
     * @return 学习建议列表
     */
    private List<String> buildSuggestions() {
        return List.of(
                "每个阶段先完成一个最小可运行示例，再补充理论细节。",
                "优先学习 AI 应用开发主线，不要过早陷入微调和推理加速等底层方向。",
                "学习资料不必全部刷完，围绕项目目标选择最适合自己的资料。",
                "工程阶段要重视接口设计、数据流、日志排查和部署验证。"
        );
    }
}
