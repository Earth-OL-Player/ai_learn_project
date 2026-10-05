# AI 服务

本服务为后端提供 AI 智能刷题评分/讨论及站内助手能力。未接入主业务的 RAG/Qdrant 预留接口已移除，当前包含：

- 健康检查：`GET /health`
- 答案评分：`POST /internal/v1/practice/answer/grade`
- 流式本题讨论：`POST /internal/v1/practice/discuss/stream`
- 站内助手分段执行：`POST /internal/v1/assistant/steps/stream`

站内助手使用 LangGraph 编排和 LangChain tool calling，复用智能刷题的模型工厂及请求级 `modelConfig`。模型仅提出受控工具调用，Java 校验身份与授权，浏览器执行本站已注册的真实表单动作；Python 不直接访问业务数据库。`LOCAL_RULE` 不提供助手问答或操作能力。

公开知识索引随源码发布。更新根目录 README 或前端公开学习资料后，在 `ai-service` 目录执行 `python -m app.assistant.build_knowledge` 重新生成索引。配置和部署说明见 [AI模型服务.md](../doc/中间件/AI模型服务.md)；本期验收步骤见 [3.当期验收文档.md](../doc/3.迭代文档/sprint202631/3.当期验收文档.md)。

## 本地启动

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:AI_SERVICE_TOKEN="AI_SERVICE_TOKEN本地占位符"
$env:AI_SERVICE_LOG_LEVEL="INFO"
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

说明：真实模型 Key 和内部 Token 只允许通过本地环境变量或私有配置注入，不得提交仓库。生产默认使用 `AI_SERVICE_LOG_LEVEL=INFO`，如本地开发需要查看大模型完整入参和返回，可临时设置为 `DEBUG`。


## Qdrant 当前状态

当前 `ai-service` 已移除 RAG/Qdrant 预留接口和 `qdrant-client` 依赖，AI 智能刷题、评分和流式讨论不依赖 Qdrant。
