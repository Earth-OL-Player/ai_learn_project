# Qdrant 中间件说明

版本：v1.3  
日期：2026-05-15  
适用工程：`ai-service`

## 1. 用途

Qdrant 是项目必选的向量数据库，用于保存学习资料、题目、标准答案、解析和知识点说明的向量索引，支撑 RAG 检索、智能出题、答案评分参考和学习建议生成。

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

当前项目代码以 `ai-service/app/config/settings.py` 为准，`ai-service` 只读取以下 Qdrant 相关配置：

| 配置项 | 示例占位符 | 说明 |
| --- | --- | --- |
| `QDRANT_URL` | `http://127.0.0.1:6333` | Qdrant HTTP 地址 |
| `QDRANT_COLLECTION` | `ai_learn_knowledge` | RAG 知识片段向量集合 |

由于 `ai-service` 的 RAG 内部接口仍需要服务间鉴权，本地启动时还需要配置内部鉴权 Token：

| 配置项 | 示例占位符 | 说明 |
| --- | --- | --- |
| `AI_SERVICE_TOKEN` | `AI_SERVICE_TOKEN本地占位符` | 调用 `ai-service` 内部 RAG 接口时使用的鉴权 Token；当前后端已移除 AI智能刷题调用，不再读取该配置 |

当前版本未读取以下环境变量，请不要作为本地启动必填项配置：

- `QDRANT_API_KEY`
- `QDRANT_COLLECTION_LEARNING`
- `QDRANT_COLLECTION_QUESTION`
- `EMBEDDING_MODEL_NAME`
- `EMBEDDING_DIMENSION`

说明：

- 当前 `QdrantClient` 只通过 `QDRANT_URL` 连接 Qdrant，暂未传入 API Key。
- 当前 RAG 入库和检索共用 `QDRANT_COLLECTION`，暂未拆分学习资料集合和题目集合。
- 当前向量集合维度由代码中的本地 Embedding 占位实现决定，后续接入真实 Embedding 模型时，需要同步修改代码和本文档。

## 6. 示例配置

本地运行时，实际生效配置文件为 `ai-service/.env`：

```dotenv
AI_SERVICE_TOKEN=AI_SERVICE_TOKEN本地占位符
QDRANT_URL=http://127.0.0.1:6333
QDRANT_COLLECTION=ai_learn_knowledge
```

`ai-service/.env.example` 仅作为提交到仓库的模板文件，内容应与上述配置项保持一致，但不得填写真实 Token、生产连接地址等敏感信息。

占位符说明：

- `AI_SERVICE_TOKEN本地占位符`：本地可替换为自定义随机字符串，但不得提交真实生产 Token；当前仅用于 `ai-service` 内部 RAG 接口鉴权。
- `http://127.0.0.1:6333`：本地 Qdrant HTTP 地址；服务器部署时应改为内网地址或通过私有配置注入。
- `ai_learn_knowledge`：当前 RAG 入库和检索使用的统一集合名称。

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

## 9. 后续扩展配置说明

如果后续需要开启 Qdrant API Key、按业务类型拆分集合或接入真实 Embedding 模型，应先完成代码改造，再同步更新本文档和 `.env.example`。可考虑增加的配置包括：

- `QDRANT_API_KEY`：Qdrant API Key，本地未开启鉴权时可为空，生产环境可结合内网访问控制使用。
- `QDRANT_COLLECTION_LEARNING`：学习资料向量集合。
- `QDRANT_COLLECTION_QUESTION`：题目、答案和解析向量集合。
- `EMBEDDING_MODEL_NAME`：真实 Embedding 模型名称。
- `EMBEDDING_DIMENSION`：真实向量维度，必须与 Embedding 模型输出一致。

以上配置当前不是本项目本地启动必填项，避免与现有代码不一致导致误配。

## 10. sprint202608 RAG 检索说明

从 sprint202608 开始，`ai-service` 会通过 `QDRANT_URL` 连接本地 Qdrant，并使用 `QDRANT_COLLECTION` 指定集合，默认占位值如下：

```text
QDRANT_URL=http://127.0.0.1:6333
QDRANT_COLLECTION=ai_learn_knowledge
```

本地验证步骤：

1. 先按本文档启动 Qdrant。
2. 启动 `ai-service`，并配置 `AI_SERVICE_TOKEN`、`QDRANT_URL`、`QDRANT_COLLECTION`。当前后端已移除 AI智能刷题调用，联调 RAG 内部接口时由调用方携带同名 Token。
3. 调用 `POST /internal/v1/rag/index-tasks` 提交包含 `documents` 的入库请求。
4. 调用 `POST /internal/v1/rag/search` 输入 `RAG`、`Embedding` 等关键词，验证能返回知识片段。

部署注意事项：

- 生产环境 `QDRANT_URL` 必须使用服务器私有配置或环境变量注入，不得提交真实生产地址。
- Qdrant collection 中保存学习资料和题库片段的向量及元数据，需要纳入持久化和备份策略。
- 当前本地 Embedding 适配器是占位实现，后续接入真实模型时应同步更新模型 Key、超时、限流和成本控制说明。

