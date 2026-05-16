# AI模型服务配置说明

版本：v1.0  
日期：2026-05-16  
适用工程：`ai-service`、`ai-learn-backend`  
适用迭代：`sprint202612` 系统题库管理与 AI 智能刷题重构

## 1. 用途

AI模型服务是 `ai-service` 的可选外部能力，用于后续把本地规则评分升级为真实大模型评分、答案优化建议和本题讨论。当前代码已经预留 URL、Key、模型名和超时配置；本地没有真实 Key 时，会使用本地规则评分兜底。

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
5. 停止 `ai-service` 或关闭 `AI_SERVICE_ENABLED` 后，后端仍应使用本地规则兜底评分。

## 8. 后续部署到服务器注意事项

- 真实模型 Key、生产模型地址和供应商账号信息不得提交仓库。
- 生产环境应配置超时、限流、重试、成本监控和日志脱敏。
- 不得把用户完整答案和聊天记录写入日志或向量库。
- 如果模型返回结构化 JSON，需要后端或 AI 服务做字段校验和兜底，避免模型异常输出影响刷题主流程。
