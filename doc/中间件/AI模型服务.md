# AI模型服务配置说明

版本：v1.1  
日期：2026-05-17  
适用工程：`ai-service`、`ai-learn-backend`  
适用迭代：`sprint202612` 系统题库管理与 AI 智能刷题重构、`sprint2616` 答题上下文记忆与智能拦截

## 1. 用途

AI模型服务是 `ai-service` 的可选外部能力，用于把本地规则评分升级为真实大模型评分、答案优化建议、本题讨论和刷题输入相关性判断。当前代码已经预留 URL、Key、模型名和超时配置；本地没有真实 Key 时，会使用本地规则评分或保守拦截兜底。

## 2. 推荐版本

本仓库不绑定具体模型供应商版本。生产接入时建议选择兼容 OpenAI Chat Completions 风格的稳定模型服务，并在私有部署文档中记录供应商、模型名、上下文长度、价格、限流和 SLA。

## 3. 本地安装方式

无需额外安装本地中间件。开发者只需要启动 `ai-service`；如果需要真实模型评分，请在本地环境变量中配置模型服务地址和 Key。

## 4. 本地启动方式

按根目录 `README.md` 启动 `ai-service`。未配置真实模型服务时，保留如下占位配置即可：

```dotenv
AI_GRADING_BASE_URL=https://模型服务地址占位符/v1/chat/completions
AI_GRADING_API_KEY=AI_GRADING_API_KEY占位符
AI_GRADING_MODEL=LOCAL_RULE
AI_GRADING_TIMEOUT_SECONDS=20
```

## 5. 必要配置项

| 配置项 | 示例占位符 | 说明 |
| --- | --- | --- |
| `AI_SERVICE_TOKEN` | `AI_SERVICE_TOKEN本地占位符` | 后端调用 AI 服务内部接口的鉴权 Token |
| `AI_SERVICE_ENABLED` | `true` | 后端是否调用 AI 服务；关闭时使用后端本地规则兜底 |
| `AI_SERVICE_BASE_URL` | `http://127.0.0.1:8000` | 后端访问 AI 服务的基础地址 |
| `AI_SERVICE_TIMEOUT_SECONDS` | `15` | 后端调用 AI 服务超时时间 |
| `AI_GRADING_BASE_URL` | `https://模型服务地址占位符/v1/chat/completions` | 可选真实模型服务地址 |
| `AI_GRADING_API_KEY` | `AI_GRADING_API_KEY占位符` | 可选真实模型服务 Key |
| `AI_GRADING_MODEL` | `LOCAL_RULE` | 本地规则或真实模型名 |
| `AI_GRADING_TIMEOUT_SECONDS` | `20` | AI 服务调用模型服务超时时间 |

## 6. 示例占位符配置

`ai-service/.env.example` 使用占位符，不包含真实密钥：

```dotenv
AI_SERVICE_TOKEN=AI_SERVICE_TOKEN占位符
QDRANT_URL=http://127.0.0.1:6333
QDRANT_COLLECTION=ai_learn_knowledge
AI_GRADING_BASE_URL=https://模型服务地址占位符/v1/chat/completions
AI_GRADING_API_KEY=AI_GRADING_API_KEY占位符
AI_GRADING_MODEL=LOCAL_RULE
AI_GRADING_TIMEOUT_SECONDS=20
```

## 7. 验证方式

1. 启动 MySQL、Qdrant、`ai-service`、后端和前端。
2. 登录平台并进入 AI 智能刷题页面。
3. 点击“开始刷题”，提交一段答案。
4. 页面应展示评分、参考答案、命中点、缺失点和优化建议。
5. 评分后追问“我刚刚回答的答案是什么”，真实模型启用时应能结合当前题最近答案回复。
6. 在答题或讨论阶段输入明显无关内容，真实模型启用时应优先由 `/internal/v1/practice/relevance` 判断是否拦截。
7. 停止 `ai-service` 或关闭 `AI_SERVICE_ENABLED` 后，后端仍应使用本地规则兜底评分，并用关键词兜底拦截明显无关内容。

## 8. 后续部署到服务器注意事项

- 真实模型 Key、生产模型地址和供应商账号信息不得提交仓库。
- 生产环境应配置超时、限流、重试、成本监控和日志脱敏。
- 不得把用户完整答案和聊天记录写入日志或向量库。
- 如果模型返回结构化 JSON，需要后端或 AI 服务做字段校验和兜底，避免模型异常输出影响刷题主流程。

## 9. sprint202614 本地联调和 422 排查补充

本期修复了 Java 后端调用 Python AI 服务时，本地 Uvicorn 对明文 HTTP/2 h2c 升级请求兼容不足可能导致请求体丢失的问题。当前后端到 `ai-service` 的内部调用要求如下：

- Java 后端固定使用 HTTP/1.1 调用 `ai-service`，避免本地 Uvicorn 出现 `Unsupported upgrade request` 后影响请求体解析。
- 请求头使用 `Content-Type: application/json; charset=utf-8`，请求体统一使用 UTF-8 JSON。
- `AI_SERVICE_TOKEN` 必须在 Java 后端和 Python AI 服务两侧保持一致；示例值只能使用占位符，真实值请在本地或服务器环境变量中配置。
- 如果 Python 日志出现 422 参数校验失败，请优先检查 `contentType`、`contentLength`、`AI_SERVICE_ENABLED`、`AI_SERVICE_BASE_URL` 和内部 Token 是否配置正确。
- 排查日志不得打印用户答案全文、真实 Token、真实模型 Key 或生产服务地址。

本地验证建议：

1. 启动 `ai-service` 后访问 `http://127.0.0.1:8000/health`，确认返回 `UP`。
2. 启动 Java 后端并配置 `AI_SERVICE_ENABLED=true`、`AI_SERVICE_BASE_URL=http://127.0.0.1:8000`。
3. 在前端 AI 智能刷题页面提交答案。
4. Python 日志应能看到 `/internal/v1/practice/answer/grade` 返回 200；如果真实模型未配置，Python 会使用本地规则评分。
5. 如果停止 Python AI 服务，Java 后端应自动切换为本地评分兜底，前端仍能看到评分结果。

## 10. sprint2616 答题上下文与智能拦截补充

本期 AI 服务新增和调整以下内部能力：

| 内部接口 | 用途 |
| --- | --- |
| `POST /internal/v1/practice/discuss` | 请求字段新增 `lastUserAnswer`，用于本题讨论阶段让模型记住当前题最近一次用户答案 |
| `POST /internal/v1/practice/discuss/stream` | 使用 OpenAI 兼容 `stream=true` 调用模型，并把可见文本片段以 SSE 返回给 Java 后端 |
| `POST /internal/v1/practice/relevance` | 判断用户输入是否与刷题、当前题回答或技术讨论相关 |

本地联调注意事项：

1. `lastUserAnswer` 由 Java 后端从 MySQL 当前会话字段读取并传入，AI 服务不持久化该内容。
2. 相关性判断要求模型只返回 JSON：`relevant` 和 `reason`。
3. 讨论阶段流式输出依赖模型服务支持 OpenAI 兼容 Chat Completions SSE 响应，即返回 `data: {...}` 和最终 `data: [DONE]`。
4. 模型不可用、Key 使用占位符、模型名为 `LOCAL_RULE` 或解析失败时，AI 服务回退到本地保守规则。
5. 日志只能记录 traceId、场景、模型名、耗时和响应预览，禁止打印真实 Key、完整用户答案或完整提示词。
6. 生产部署时如接入第三方模型，需要确认供应商的数据使用、留存和脱敏策略符合项目要求。
