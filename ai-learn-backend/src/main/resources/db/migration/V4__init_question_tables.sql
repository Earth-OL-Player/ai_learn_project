CREATE TABLE questions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    code VARCHAR(64) NOT NULL COMMENT '题目编码',
    question TEXT NOT NULL COMMENT '题目',
    question_type VARCHAR(32) NOT NULL COMMENT '题目分类',
    standard_answer TEXT NOT NULL COMMENT '参考答案',
    importance_score INT NOT NULL DEFAULT 60 COMMENT '重要性评分，百分制',
    occurrence_count INT NOT NULL DEFAULT 0 COMMENT '真实面试出现次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_questions_code (code),
    KEY idx_questions_question_type (question_type),
    KEY idx_questions_type_importance (question_type, importance_score),
    KEY idx_questions_code_deleted (code, deleted),
    KEY idx_questions_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

CREATE TABLE knowledge_points (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '知识点ID',
    name VARCHAR(80) NOT NULL COMMENT '知识点名称',
    description VARCHAR(500) NULL COMMENT '知识点说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_points_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点表';

CREATE TABLE question_knowledge_points (
    question_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    knowledge_point_id BIGINT UNSIGNED NOT NULL COMMENT '知识点ID',
    PRIMARY KEY (question_id, knowledge_point_id),
    CONSTRAINT fk_question_knowledge_points_question_id FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_question_knowledge_points_knowledge_point_id FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_points (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目知识点关系表';

INSERT INTO knowledge_points(name, description) VALUES
('RAG', '检索增强生成，通过外部知识检索增强大模型回答质量'),
('Embedding', '把文本转换为向量表示，支持语义相似度计算'),
('向量数据库', '用于存储和检索向量数据的数据库，例如 Qdrant、Milvus'),
('LangChain', '大模型应用编排框架，支持链、工具和 Agent 编排'),
('Agent', '能够规划步骤、调用工具并根据结果继续推理的大模型应用形态'),
('Prompt Engineering', '通过提示词结构化设计提升大模型输出质量'),
('大模型应用架构', '围绕模型、数据、工具、评估和安全构建应用系统');

INSERT INTO questions(code, question, question_type, standard_answer, importance_score, occurrence_count) VALUES
('SYS-RAG-0001',
 '请说明 RAG 的基本流程，并解释它相比只使用大模型参数回答的优势。',
 'RAG',
 'RAG 是检索增强生成。典型流程是先根据用户问题检索外部知识，再把检索结果和问题一起交给大模型生成答案。它可以降低幻觉、补充私有或实时知识，并提升回答可追溯性。',
 90, 120),
('SYS-EMBEDDING-0001',
 '请解释 Embedding 的含义，并说明为什么它适合用于相似问题、文档片段召回。',
 'Embedding',
 'Embedding 将文本映射为向量，语义相近的文本在向量空间距离更近。语义检索可以通过余弦相似度等方式找到含义接近的内容，而不仅依赖关键词完全匹配。',
 82, 86),
('SYS-VECTOR-0001',
 '请结合文档切片、向量化、召回三个环节说明向量数据库在 RAG 系统中的作用。',
 '向量数据库',
 '向量数据库负责保存文档片段的向量和元数据，并在用户问题向量化后执行近邻检索，返回语义相关片段给生成模型使用。它通常还支持过滤、索引和批量更新。',
 86, 73),
('SYS-LANGCHAIN-0001',
 '请说明 LangChain 在模型调用、提示词、工具、链路编排中的作用。',
 'LangChain',
 'LangChain 提供模型、Prompt、Retriever、Tool、Chain、Agent 等抽象，帮助开发者把多个组件组合成完整大模型应用，减少重复胶水代码。',
 78, 65),
('SYS-AGENT-0001',
 '请从目标拆解、工具调用、执行反馈三个角度说明 Agent 与普通 Chatbot 的区别。',
 'Agent',
 '普通 Chatbot 通常直接基于对话上下文回复。Agent 会围绕目标进行步骤规划，必要时调用搜索、数据库、代码执行等工具，并根据工具结果继续决策。',
 88, 110),
('SYS-RAG-ARCH-0001',
 '请描述从知识入库到在线问答的基础 RAG 系统关键模块。',
 '大模型应用架构',
 '基础 RAG 系统通常包括文档采集、清洗切片、Embedding、向量入库、在线问题向量化、相似片段召回、Prompt 组装、模型生成和答案展示。还应关注权限、监控、评估和更新机制。',
 95, 96),
('SYS-PROMPT-0001',
 '请说明为什么 Prompt Engineering 中要给出角色、约束和输出格式。',
 'Prompt Engineering',
 '角色可以限定回答视角，约束可以明确边界和禁止事项，输出格式可以提升结果可解析性和一致性。结构化提示词能减少歧义，提高模型输出稳定性。',
 70, 58),
('SYS-LLM-RISK-0001',
 '请从数据安全、幻觉、成本、稳定性和观测角度说明大模型应用上线前需要关注哪些基础风险。',
 '大模型应用架构',
 '上线前应关注敏感数据保护、权限控制、幻觉与错误答案、Token 成本、限流降级、模型超时、日志观测、质量评估和用户反馈闭环。',
 92, 104);

INSERT INTO question_knowledge_points(question_id, knowledge_point_id)
SELECT q.id, kp.id FROM questions q JOIN knowledge_points kp ON q.code = 'SYS-RAG-0001' AND kp.name IN ('RAG', '大模型应用架构');
INSERT INTO question_knowledge_points(question_id, knowledge_point_id)
SELECT q.id, kp.id FROM questions q JOIN knowledge_points kp ON q.code = 'SYS-EMBEDDING-0001' AND kp.name IN ('Embedding');
INSERT INTO question_knowledge_points(question_id, knowledge_point_id)
SELECT q.id, kp.id FROM questions q JOIN knowledge_points kp ON q.code = 'SYS-VECTOR-0001' AND kp.name IN ('向量数据库', 'RAG');
INSERT INTO question_knowledge_points(question_id, knowledge_point_id)
SELECT q.id, kp.id FROM questions q JOIN knowledge_points kp ON q.code = 'SYS-LANGCHAIN-0001' AND kp.name IN ('LangChain', '大模型应用架构');
INSERT INTO question_knowledge_points(question_id, knowledge_point_id)
SELECT q.id, kp.id FROM questions q JOIN knowledge_points kp ON q.code = 'SYS-AGENT-0001' AND kp.name IN ('Agent');
INSERT INTO question_knowledge_points(question_id, knowledge_point_id)
SELECT q.id, kp.id FROM questions q JOIN knowledge_points kp ON q.code = 'SYS-RAG-ARCH-0001' AND kp.name IN ('RAG', '向量数据库', '大模型应用架构');
INSERT INTO question_knowledge_points(question_id, knowledge_point_id)
SELECT q.id, kp.id FROM questions q JOIN knowledge_points kp ON q.code = 'SYS-PROMPT-0001' AND kp.name IN ('Prompt Engineering');
INSERT INTO question_knowledge_points(question_id, knowledge_point_id)
SELECT q.id, kp.id FROM questions q JOIN knowledge_points kp ON q.code = 'SYS-LLM-RISK-0001' AND kp.name IN ('大模型应用架构');
