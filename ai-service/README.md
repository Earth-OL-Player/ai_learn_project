# AI 服务

本服务为后端提供内部 AI 能力，当前包含：

- 健康检查：`GET /health`
- 答案评分：`POST /internal/v1/agent/answer/grade`
- RAG 入库任务：`POST /internal/v1/rag/index-tasks`
- RAG 任务查询：`GET /internal/v1/rag/index-tasks/{task_id}`
- RAG 检索：`POST /internal/v1/rag/search`

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
