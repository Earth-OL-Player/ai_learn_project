# AI 学习项目

本项目当前已形成“前端学习平台 + Java 业务后端 + Python AI 服务”的可运行闭环，核心能力包括学习路线展示、用户认证、建议评论区、热门面经、系统题库管理、AI 智能刷题、AI 评分/讨论、成长等级/段位/勋章和 RAG 入库检索。学习路线页面继续使用前端项目内 Markdown 文件静态渲染。

## 项目结构

| 目录 | 说明 |
| --- | --- |
| `ai-learn-backend` | Spring Boot 后端服务，提供认证、用户、互动、系统题库、AI刷题、RAG 任务、成长体系和管理后台接口。 |
| `ai-learn-web` | Vue 3 + Vite 前端项目，提供清新简约的学习平台、刷题、个人中心和管理者中心页面。 |
| `ai-service` | FastAPI AI 服务，提供 AI 评分和 RAG 入库相关能力。 |


## 项目启动步骤

### 1. 本地环境准备

建议本地准备以下运行环境：

| 环境 | 推荐版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | 运行 `ai-learn-backend`。 |
| Maven | 3.9.x 或兼容版本 | 构建和启动 Spring Boot 后端。 |
| Node.js | 20 LTS 或 22 LTS | 运行 `ai-learn-web`。 |
| Python | 3.11+ | 运行 `ai-service`。 |
| MySQL | 8.4 LTS | 保存用户、系统题库、刷题汇总、RAG 任务和成长数据。 |
| Qdrant | 1.x | 启用 RAG 入库/检索时必选；默认用于知识片段向量索引。 |

中间件安装、启动和部署注意事项请优先查看：

- [MySQL 本地与部署说明](doc/中间件/MySQL.md)
- [Qdrant 本地与部署说明](doc/中间件/Qdrant.md)
- [AI模型服务配置说明](doc/中间件/AI模型服务.md)
- [Redis 本地与部署说明](doc/中间件/Redis.md)

说明：基础登录、题库、互动和刷题功能依赖 MySQL；启用 AI 评分/讨论需启动 `ai-service`；启用 RAG 入库/检索需同时启动 Qdrant。Redis 当前未接入运行代码，文档仅作为后续缓存/限流能力预留参考。所有真实密码、Token、密钥和生产连接地址都只能保存在本地私有配置中，禁止提交到仓库。

### 2. 准备后端配置

后端读取系统环境变量，建议本地维护 `ai-learn-backend/.env` 作为占位配置来源；使用 IDE 启动时可将这些键值导入运行配置，使用 PowerShell 启动时可先将 `.env` 加载到当前进程环境变量。

`ai-learn-backend/.env` 示例：

```env
DATABASE_URL="jdbc:mysql://127.0.0.1:3306/ai_learn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
DATABASE_USERNAME="本地MySQL用户名占位符"
DATABASE_PASSWORD="本地MySQL密码占位符"
SPRING_FLYWAY_ENABLED="true"
JWT_SECRET="至少32位本地JWT密钥占位符"
JWT_EXPIRES_IN_SECONDS="7200"

# 启用 AI 服务时，请保持 token 与 ai-service/.env 一致。
AI_SERVICE_ENABLED="true"
AI_SERVICE_BASE_URL="http://127.0.0.1:8000"
AI_SERVICE_TOKEN="AI_SERVICE_TOKEN本地占位符"
AI_SERVICE_TIMEOUT_SECONDS="15"
```

PowerShell 临时加载 `.env` 示例：

```powershell
cd ai-learn-backend
Get-Content .\.env | Where-Object { $_ -and $_ -notmatch '^\s*#' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim().Trim('"'), 'Process')
}
```

说明：Flyway 默认启用，后端启动后会自动执行 `src/main/resources/db/migration` 下全部数据库迁移，初始化用户、互动、题库、刷题会话、RAG 任务、成长徽章、系统设置和超级管理员标识相关表结构。

### 3. 准备前端配置

`ai-learn-web/.env` 示例：

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### 4. 准备 AI 服务配置

启动项目前必须配置 `ai-service/.env`：

```env
AI_SERVICE_TOKEN=AI_SERVICE_TOKEN本地占位符
QDRANT_URL=http://127.0.0.1:6333
QDRANT_COLLECTION=ai_learn_knowledge
AI_GRADING_BASE_URL=https://模型服务地址占位符/v1/chat/completions
AI_GRADING_API_KEY=AI_GRADING_API_KEY占位符
AI_GRADING_MODEL=LOCAL_RULE
AI_GRADING_TIMEOUT_SECONDS=20
```

说明：`AI_GRADING_MODEL=LOCAL_RULE` 表示使用本地规则兜底能力；如果后续接入真实模型，`AI_GRADING_API_KEY` 必须替换为本地私有值，禁止提交真实密钥。

### 5. 启动顺序

#### 5.1 启动 MySQL

按 [MySQL 本地与部署说明](doc/中间件/MySQL.md) 创建本地数据库和业务账号，确保 `DATABASE_URL`、`DATABASE_USERNAME`、`DATABASE_PASSWORD` 与本地配置一致。

#### 5.2 启动 Qdrant

Qdrant 为本地完整启动必选中间件，启动方式见 [Qdrant 本地与部署说明](doc/中间件/Qdrant.md)。

#### 5.3 启动 AI 服务

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

健康检查地址：

```text
http://localhost:8000/health
```

预期结果：返回 `status=UP`。

#### 5.4 启动后端

如果使用 PowerShell 启动，请先按“准备后端配置”章节加载环境变量，然后执行：

```powershell
cd ai-learn-backend
mvn spring-boot:run
```

后端访问地址：

```text
http://localhost:8080
```

健康检查：

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health" -Method Get
```

预期结果：后端启动成功，健康检查正常返回；首次连接 MySQL 时 Flyway 自动完成数据库表结构初始化。

#### 5.5 启动前端

```powershell
cd ai-learn-web
npm install
npm run dev
```

前端访问地址：

```text
http://localhost:5173
```

### 6. 常用验收检查

1. 访问 `http://localhost:5173`，确认前端页面可以正常打开。
2. 调用 `http://localhost:8080/api/v1/health`，确认后端健康检查正常。
3. 访问 `http://localhost:8000/health`，确认 AI 服务健康检查正常。
4. 确认 Qdrant 已启动，且 `QDRANT_COLLECTION` 与 `ai-service/.env` 保持一致。
5. 在 MySQL 中执行 `SHOW TABLES;`，确认 Flyway 已初始化当前迭代所需业务表。

说明：本项目当前要求禁止新增单元测试代码，验收过程只记录构建、接口联调和人工验收步骤。
