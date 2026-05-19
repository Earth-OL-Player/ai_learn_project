# AI 服务

本服务为后端提供内部 AI 能力，当前主业务使用 AI 智能刷题评分/讨论能力。未接入主业务的 RAG/Qdrant 预留接口已移除，当前包含：

- 健康检查：`GET /health`
- 答案评分：`POST /internal/v1/practice/answer/grade`
- 流式本题讨论：`POST /internal/v1/practice/discuss/stream`

## 本地启动

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:AI_SERVICE_TOKEN="AI_SERVICE_TOKEN本地占位符"
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

说明：真实模型 Key 和内部 Token 只允许通过本地环境变量或私有配置注入，不得提交仓库。


## Qdrant 当前状态

当前 `ai-service` 已移除 RAG/Qdrant 预留接口和 `qdrant-client` 依赖，AI 智能刷题、评分和流式讨论不依赖 Qdrant。
