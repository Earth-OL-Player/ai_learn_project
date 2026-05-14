# Qdrant 中间件说明

版本：v1.1  
日期：2026-05-14  
适用工程：`ai-service`

## 1. 用途

Qdrant 是项目的向量数据库，用于保存学习资料、题目、标准答案、解析和知识点说明的向量索引，支撑 RAG 检索、智能出题、答案评分参考和学习建议生成。

边界说明：

- Qdrant 只保存向量、文本片段和必要 metadata。
- 用户、题库、答题记录等业务主数据仍保存在 MySQL。
- 不在 Qdrant payload 中保存真实密码、Token、API Key、用户隐私等敏感信息。

## 2. 推荐版本

推荐使用：

```text
Qdrant 1.x 稳定版
```

说明：

- 服务端版本和 `qdrant-client` 版本需要保持兼容。
- 本地和生产尽量保持同一主版本。
- 向量维度必须与 Embedding 模型输出维度一致；维度变化通常需要新建集合并重新入库。

## 3. 本地安装方式

### 3.1 Docker 方式（推荐）

```bash
docker run --name ai-learn-qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  -v ai-learn-qdrant-data:/qdrant/storage \
  -d qdrant/qdrant:${QDRANT_IMAGE_TAG}
```

占位符说明：

- `${QDRANT_IMAGE_TAG}`：Qdrant 1.x 稳定版本标签；本地临时验证可使用 `latest`，生产环境必须固定明确版本。

端口说明：

- `6333`：HTTP API 端口。
- `6334`：gRPC 端口，MVP 阶段可暂不使用。

### 3.2 本机安装方式

也可以使用 Qdrant 官方二进制或包管理器安装。

要求：

- HTTP 服务默认监听 `6333`。
- 数据目录需要持久化。
- 如开启 API Key，同步配置 `QDRANT_API_KEY`。

## 4. 本地启动方式

启动容器：

```bash
docker start ai-learn-qdrant
```

停止容器：

```bash
docker stop ai-learn-qdrant
```

查看日志：

```bash
docker logs -f ai-learn-qdrant
```

## 5. 必要配置项

`ai-service` 需要以下配置：

| 配置项 | 示例占位符 | 说明 |
| --- | --- | --- |
| `QDRANT_URL` | `http://127.0.0.1:6333` | Qdrant HTTP 地址 |
| `QDRANT_API_KEY` | `${QDRANT_API_KEY}` | Qdrant API Key，本地未开启鉴权时可为空 |
| `QDRANT_COLLECTION_LEARNING` | `ai_learn_learning_chunks` | 学习资料向量集合 |
| `QDRANT_COLLECTION_QUESTION` | `ai_learn_question_chunks` | 题目、答案和解析向量集合 |
| `EMBEDDING_MODEL_NAME` | `${EMBEDDING_MODEL_NAME}` | Embedding 模型名称 |
| `EMBEDDING_DIMENSION` | `${EMBEDDING_DIMENSION}` | 向量维度，必须与模型输出一致 |

## 6. 示例配置

`.env.example`：

```dotenv
QDRANT_URL=http://127.0.0.1:6333
QDRANT_API_KEY=${QDRANT_API_KEY_LOCAL_OPTIONAL}
QDRANT_COLLECTION_LEARNING=ai_learn_learning_chunks
QDRANT_COLLECTION_QUESTION=ai_learn_question_chunks
EMBEDDING_MODEL_NAME=${EMBEDDING_MODEL_NAME}
EMBEDDING_DIMENSION=${EMBEDDING_DIMENSION}
```

占位符说明：

- `${QDRANT_API_KEY_LOCAL_OPTIONAL}`：本地未开启鉴权时可为空；生产环境建议配置真实 API Key 或内网访问控制。
- `${EMBEDDING_MODEL_NAME}`：真实 Embedding 模型名称，由开发者或部署环境配置。
- `${EMBEDDING_DIMENSION}`：真实向量维度，必须与 Embedding 模型一致。

## 7. 验证方式

健康检查：

```bash
curl http://127.0.0.1:6333/healthz
```

集合列表：

```bash
curl http://127.0.0.1:6333/collections
```

AI 服务验证：

- 启动 `ai-service`。
- 调用 RAG 入库任务接口提交少量测试文档。
- 查询 Qdrant collections，确认集合创建成功。
- 调用检索接口，确认能返回相关知识片段。

## 8. 生产部署注意事项

- Qdrant 端口不直接暴露公网。
- 生产环境优先通过内网、安全组、反向代理或 API Key 控制访问。
- 数据目录必须持久化，并制定备份策略。
- Embedding 模型切换前先确认向量维度；维度变化时新建集合并重新入库。
- metadata 控制大小，只保存检索过滤必要信息，例如知识点、难度、题型、来源 ID。
- 不在 payload 中保存密码、Token、API Key、用户隐私等敏感信息。
- RAG 入库采用异步任务，避免大量切分和向量化导致接口超时。
- 生产镜像标签必须固定明确版本，不使用浮动 `latest`。
