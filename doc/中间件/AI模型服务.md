# AI模型服务配置说明

版本：v1.11
日期：2026-05-24
适用工程：`ai-service`、`ai-learn-backend`  
适用迭代：`sprint202612` 系统题库管理与 AI 智能刷题重构、`sprint2616` 答题上下文记忆与智能拦截、`sprint2622` LangChain Agent 化与多轮记忆、`sprint202627` 模型权益和请求级模型配置

## 1. 用途

AI模型服务是 `ai-service` 的可选外部能力，用于把本地规则评分升级为真实大模型评分、答案优化建议和本题多轮讨论。当前 AI 智能刷题、评分和讨论不依赖 Qdrant，未接入主业务的 RAG/Qdrant 预留代码、配置和依赖已移除。当前代码通过 LangChain `init_chat_model` 接入模型，公共占位符与 LOCAL_RULE 常量集中在 `app/config/constants.py`；答案评分使用结构化响应返回 JSON，讨论能力继续使用 `create_agent`。本地没有真实 Key 时，会使用本地规则评分或讨论不可用提示兜底。明显无关问题已改由 Java 后端本地关键词拦截，不再额外调用模型判断相关性。`sprint202627` 后，Java 后端会按用户当前模型权益把请求级 `modelConfig` 传给 `ai-service`，用于区分初级、高级和超级模型调用配置。

## 2. 推荐版本

本仓库不绑定具体模型供应商版本。生产接入时建议选择 LangChain 支持的稳定聊天模型供应商，并在私有部署文档中记录供应商、模型名、上下文长度、价格、限流和 SLA。默认不强制指定 `AI_GRADING_MODEL_PROVIDER`，优先让 LangChain 根据模型名推断；无法推断或需要指定供应商时再显式配置，例如 OpenAI 兼容服务可使用 `openai`。

接入 DeepSeek 时，本地示例可使用 `AI_GRADING_BASE_URL=https://api.deepseek.com` 或供应商官方文档要求的兼容基础地址；如果 LangChain 无法根据模型名推断供应商，可显式配置 `AI_GRADING_MODEL_PROVIDER=deepseek`。当前代码识别到 DeepSeek 供应商、DeepSeek 模型名或 DeepSeek 官方域名后，会通过 LangChain `extra_body` 传入 `{"thinking":{"type":"disabled"}}`，关闭 DeepSeek V4 默认思考模式。评分链路禁止依赖真实 Key 入库，真实 Key 只允许放在本地 `.env`、服务器环境变量或密钥系统中。

## 3. 本地安装方式

无需额外安装本地中间件。开发者只需要启动 `ai-service`；如果需要真实模型评分，请在本地环境变量中配置模型服务地址和 Key。

## 4. 本地启动方式

按根目录 `README.md` 启动 `ai-service`。未配置真实模型服务时，保留如下占位配置即可：

```dotenv
AI_SERVICE_LOG_LEVEL=INFO
AI_GRADING_BASE_URL=https://模型服务地址占位符/v1
AI_GRADING_API_KEY=AI_GRADING_API_KEY占位符
AI_GRADING_MODEL=LOCAL_RULE
AI_GRADING_MODEL_PROVIDER=
AI_GRADING_TIMEOUT_SECONDS=20
AI_GRADING_MAX_OUTPUT_TOKENS=800
```

`ai-learn-backend` 授权入口使用环境变量占位符：

```dotenv
MODEL_AUTHORIZATION_URL=https://authorization.example.com/model-auth
```

占位符说明：真实授权入口地址必须是包含 `http://` 或 `https://` 的完整网站地址，只能配置在本地环境变量或服务器私有配置中，禁止提交生产地址或带 Token 的链接。

## 5. 必要配置项

| 配置项 | 示例占位符 | 说明 |
| --- | --- | --- |
| `AI_SERVICE_TOKEN` | `AI_SERVICE_TOKEN本地占位符` | 后端调用 AI 服务内部接口的鉴权 Token |
| `AI_SERVICE_LOG_LEVEL` | `INFO` | AI 服务日志级别；生产默认 `INFO`，本地开发排查大模型完整入参和返回时可临时改为 `DEBUG` |
| `AI_SERVICE_ENABLED` | `true` | 后端是否调用 AI 服务；关闭时使用后端本地规则兜底 |
| `AI_SERVICE_BASE_URL` | `http://127.0.0.1:8000` | 后端访问 AI 服务的基础地址 |
| `AI_SERVICE_TIMEOUT_SECONDS` | `15` | 后端调用 AI 服务超时时间 |
| `RATE_LIMIT_AI_REQUEST_LIMIT` | `8` | Java 后端 AI 评分/讨论流式入口在窗口内允许的请求次数 |
| `RATE_LIMIT_AI_REQUEST_WINDOW_SECONDS` | `60` | Java 后端 AI 评分/讨论流式入口频率限流窗口秒数 |
| `RATE_LIMIT_AI_CONCURRENT_LIMIT` | `1` | Java 后端单用户 AI 评分/讨论流式入口并发上限 |
| `MODEL_AUTHORIZATION_URL` | `https://authorization.example.com/model-auth` | Java 后端模型授权入口完整网站地址，必须包含 `http://` 或 `https://`；前端授权按钮会按该地址新标签打开，未配置或格式不合法时提示暂未开放 |
| `AI_GRADING_BASE_URL` | `https://模型服务地址占位符/v1` | 可选真实模型服务基础地址，OpenAI 兼容服务通常填写到 `/v1` |
| `AI_GRADING_API_KEY` | `AI_GRADING_API_KEY占位符` | 可选真实模型服务 Key |
| `AI_GRADING_MODEL` | `LOCAL_RULE` | 本地规则或真实模型名 |
| `AI_GRADING_MODEL_PROVIDER` | 空字符串 | 可选 LangChain 模型供应商标识；为空时让 LangChain 根据模型名推断，无法推断时再按官方文档配置 |
| `AI_GRADING_TIMEOUT_SECONDS` | `20` | AI 服务调用模型服务超时时间 |
| `AI_GRADING_MAX_OUTPUT_TOKENS` | `800` | AI 服务调用模型服务时允许单次回复生成的最大输出 Token 数 |

## 6. 示例占位符配置

`ai-service/.example.env` 使用占位符，不包含真实密钥：

```dotenv
AI_SERVICE_TOKEN=AI_SERVICE_TOKEN占位符
AI_SERVICE_LOG_LEVEL=INFO
AI_GRADING_BASE_URL=https://模型服务地址占位符/v1
AI_GRADING_API_KEY=AI_GRADING_API_KEY占位符
AI_GRADING_MODEL=LOCAL_RULE
AI_GRADING_MODEL_PROVIDER=
AI_GRADING_TIMEOUT_SECONDS=20
AI_GRADING_MAX_OUTPUT_TOKENS=800
```

## 7. 验证方式

1. 启动 MySQL、`ai-service`、后端和前端；当前无需启动 Qdrant。
2. 登录平台并进入 AI 智能刷题页面。
3. 点击“开始刷题”，提交一段答案。
4. 页面应展示评分、参考答案、命中点、缺失点和优化建议。
5. 评分后追问“我刚刚回答的答案是什么”，真实模型启用时应能结合当前题最近答案回复。
6. 在答题或讨论阶段输入明显无关内容，应由 Java 后端本地关键词直接拦截，不再调用 AI 服务相关性接口。
7. 停止 `ai-service` 或关闭 `AI_SERVICE_ENABLED` 后，后端仍应使用本地规则兜底评分，并用关键词兜底拦截明显无关内容。
8. 打开浏览器控制台，提交一次答案或本题追问，复制日志中的 `traceId`，应能在 Java 后端日志、Python AI 服务日志和模型调用日志中按同一个 `traceId` 检索到完整链路。
9. 在管理者中心维护 PRO 或 SUPER 模型配置后，以对应权益用户提交答案，Java 后端调用 `ai-service` 的请求体应包含 `modelConfig`，Python 日志只记录模型名和 traceId，不输出真实 Key。

## 8. 后续部署到服务器注意事项

- 真实模型 Key、生产模型地址和供应商账号信息不得提交仓库。
- 生产环境应配置超时、限流、重试、成本监控和日志脱敏。
- 生产环境保持 `AI_SERVICE_LOG_LEVEL=INFO` 或更高级别，不得把用户完整答案、题目、参考答案、聊天记录和模型完整回复写入常规日志。
- 仅允许本地开发排查时临时设置 `AI_SERVICE_LOG_LEVEL=DEBUG`，排查完成后需要恢复为 `INFO`。
- Java 后端已对 AI 评分/讨论入口增加单机内存级频率和并发限流；生产多实例部署时需要迁移到 Redis、网关或其他集中式限流组件，避免各实例额度互不感知。
- 评分结构化输出通过 LangChain `with_structured_output(..., method="json_mode")` 解析，避免 DeepSeek reasoning 模型触发工具调用 `tool_choice` 兼容问题；仍需要保留兜底，避免模型异常输出影响刷题主流程。
- DeepSeek V4 默认思考模式已在客户端请求层关闭；如果服务器侧强制开启或供应商参数发生变化，需要按官方文档同步调整 `extra_body` 参数。
- Java 后端内部调用路径、Header、响应码和内容类型集中在 `AiServiceConstants`，新增 AI 内部接口时需同步维护该常量类与本文档。
- Java 后端调用 Python AI 服务时必须透传 `X-Trace-Id`；Python AI 服务响应和日志也必须继续携带同一个 `traceId`。
- 当前观测看板先以结构化日志作为数据源，字段包含评分成功率所需的 `success`、兜底率所需的 `fallbackUsed`、超时率所需的 `errorCategory=TIMEOUT`、流式首包 `firstTokenMs`、总耗时 `durationMs`、模型名、Token 用量和成本占位。生产接入 Prometheus、ELK、Grafana 或云厂商 APM 时，应直接按这些字段聚合 P95、超时率、兜底率和成本看板。

## 9. sprint202614 本地联调和 422 排查补充

本期修复了 Java 后端调用 Python AI 服务时，本地 Uvicorn 对明文 HTTP/2 h2c 升级请求兼容不足可能导致请求体丢失的问题。当前后端到 `ai-service` 的内部调用要求如下：

- Java 后端固定使用 HTTP/1.1 调用 `ai-service`，避免本地 Uvicorn 出现 `Unsupported upgrade request` 后影响请求体解析。
- 请求头使用 `Content-Type: application/json; charset=utf-8`，请求体统一使用 UTF-8 JSON。
- 请求头必须包含 `X-Trace-Id`，该值来自浏览器请求或 Java 后端过滤器生成；Python AI 服务不得重新生成覆盖已有链路标识。
- `AI_SERVICE_TOKEN` 必须在 Java 后端和 Python AI 服务两侧保持一致；示例值只能使用占位符，真实值请在本地或服务器环境变量中配置。
- 如果 Python 日志出现 422 参数校验失败，请优先检查 `contentType`、`contentLength`、`AI_SERVICE_ENABLED`、`AI_SERVICE_BASE_URL` 和内部 Token 是否配置正确。
- 排查日志不得打印用户答案全文、真实 Token、真实模型 Key 或生产服务地址。

本地验证建议：

1. 启动 `ai-service` 后访问 `http://127.0.0.1:8000/health`，确认返回 `UP`。
2. 启动 Java 后端并配置 `AI_SERVICE_ENABLED=true`、`AI_SERVICE_BASE_URL=http://127.0.0.1:8000`。
3. 在前端 AI 智能刷题页面提交答案。
4. Python 日志应能看到 `/internal/v1/practice/answer/grade` 返回 200；如果真实模型未配置，Python 会使用本地规则评分。
5. 如果停止 Python AI 服务，Java 后端应自动切换为本地评分兜底，前端仍能看到评分结果。

## 11. P1-08 AI 调用可观测性补充

本期补齐浏览器、Java 后端、Python AI 服务和模型调用日志之间的 traceId 透传与观测字段：

1. 浏览器请求统一生成 `X-Trace-Id`，普通接口和 SSE 流式接口都会在控制台记录 `traceId`、接口路径、HTTP 状态和耗时。
2. Java 后端继续通过 `TraceIdFilter` 接收或生成 traceId，并在调用 `ai-service` 的内部请求中透传 `X-Trace-Id`。
3. Python AI 服务优先使用 Java 透传的 `X-Trace-Id`，HTTP 入站日志、评分日志、流式讨论日志和模型调用日志使用同一个 traceId。
4. Java 后端新增 `AI 调用观测` 日志，字段包括 `traceId`、`path`、`stream`、`success`、`fallbackUsed`、`status`、`firstTokenMs`、`durationMs`、`model`、`inputTokens`、`outputTokens`、`totalTokens`、`estimatedCost`、`errorCategory`。
5. Python 评分接口在统一响应中返回 `observability` 元数据，Java 后端会读取其中的模型和 Token 用量并落日志；没有供应商用量时字段为 `unavailable`。
6. 当前没有引入新的中间件；后续接入指标看板时，可从 Java/Python 日志聚合 P95、评分成功率、超时率、兜底率、Token 用量和成本。

## 10. sprint2616 答题上下文与智能拦截补充

本期 AI 服务新增和调整以下内部能力：

| 内部接口 | 用途 |
| --- | --- |
| `POST /internal/v1/practice/discuss/stream` | 当前唯一讨论接口；优先使用 LangChain Agent 流式输出，如 Agent 包装层无可见 token，则切换到底层聊天模型原生 stream，并把文本片段以 SSE 返回给 Java 后端 |
| `POST /internal/v1/practice/relevance` | 已在 sprint2622 下线；明显无关问题改由 Java 后端本地关键词拦截 |

本地联调注意事项：

1. `lastUserAnswer` 由 Java 后端从 MySQL 当前会话字段读取并传入，AI 服务不持久化该内容。
2. 相关性判断接口已下线；明显无关问题由 Java 后端本地关键词拦截。
3. 讨论阶段流式输出依赖 LangChain 对应模型集成支持流式消息。
4. 模型不可用、Key 使用占位符、模型名为 `LOCAL_RULE` 或解析失败时，AI 服务回退到本地保守规则。
5. 日志只能记录 traceId、场景、模型名、耗时和响应预览，禁止打印真实 Key、完整用户答案或完整提示词。
6. 生产部署时如接入第三方模型，需要确认供应商的数据使用、留存和脱敏策略符合项目要求。Qdrant 当前不是项目运行依赖，无需部署。

## 12. sprint202627 请求级模型配置补充

本迭代新增模型权益后，Java 后端会在评分和本题讨论请求中追加 `modelConfig`：

```json
{
  "modelConfig": {
    "model": "deepseek-v4-pro",
    "baseUrl": "https://模型服务地址占位符/v1",
    "apiKey": "AI_GRADING_API_KEY占位符"
  }
}
```

处理规则：

1. `ai-service` 优先读取请求体中的 `modelConfig.model`、`modelConfig.baseUrl` 和 `modelConfig.apiKey`。
2. 请求级配置缺失或不完整时，回退到 `ai-service/.env` 中的 `AI_GRADING_MODEL`、`AI_GRADING_BASE_URL` 和 `AI_GRADING_API_KEY`。
3. 请求级 `apiKey` 只允许在 Java 后端到 Python AI 服务的内网请求体中传递，不得写入日志。
4. Python 日志只输出 traceId、模型名、是否启用真实模型和耗时，不输出真实 Key。
5. 管理端模型配置保存到 MySQL `model_configs`，本地联调时可使用占位符；真实值只允许在本地私有环境或服务器管理端维护。
6. `MODEL_AUTHORIZATION_URL` 仅控制前端授权按钮跳转地址，不参与模型调用鉴权，必须配置为完整 `http/https` 网站地址，不得配置相对路径或带密钥的 URL。
