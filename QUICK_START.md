# AI 学习项目

本项目当前已形成“前端学习平台 + Java 业务后端 + Python AI 服务”的可运行闭环，核心能力包括学习路线展示、用户认证、建议评论区、热门面经、系统题库管理、AI 智能刷题、AI 评分/讨论、成长等级/段位/勋章。当前已移除未接入主业务的 RAG/Qdrant 预留代码，学习路线页面继续使用前端项目内 Markdown 文件静态渲染。

## 项目结构

| 目录 | 说明 |
| --- | --- |
| `ai-learn-backend` | Spring Boot 后端服务，提供认证、用户、互动、系统题库、AI刷题、成长体系和管理后台接口。 |
| `ai-learn-web` | Vue 3 + Vite 前端项目，提供清新简约的学习平台、刷题、个人中心和管理者中心页面。 |
| `ai-service` | FastAPI AI 服务，提供 AI 评分/讨论能力。 |


## 项目启动步骤

### 当前 Windows 本机：极简启动

本机配置已经填入 `ai-learn-backend/.env`、`ai-service/.env`、`ai-learn-web/.env.local` 和 `.local/dev.config.json`。模型使用 `gpt-6-luna`，通过用户指定的 OpenAI 兼容服务调用，`AI_GRADING_MODEL_PROVIDER=openai`；内部 Token、JWT 和本地数据库账号密码已配置一致。脚本使用已有的 MySQL 8.4、Python 虚拟环境、前端依赖以及 IDE 自带 JDK/Maven，不需要重新安装。

在项目根目录打开 PowerShell：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-dev.ps1
```

打开 **http://127.0.0.1:5173**，注册账号并登录即可验证。

停止、重新启动及查看状态：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-dev.ps1 -Action Stop
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-dev.ps1 -Action Restart
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-dev.ps1 -Action Status
```

启动顺序为 MySQL → Python → Java → 前端。首次启动自动创建本地 `ai_learn` 数据库和业务账号，Java Flyway 自动迁移到当前版本；重复启动保留本地数据。Java 默认先构建最新代码，前端开发服务器支持页面热更新；修改 Java/Python 或 `.env` 后使用 Restart。停止只处理该脚本记录的进程，数据库正常关闭、数据保留。

| 服务 | 本机地址 | 配置/日志 |
| --- | --- | --- |
| MySQL 8.4 | `127.0.0.1:3307`，库 `ai_learn` | 账号密码在后端 `.env`；数据 `.local/mysql-data` |
| Python AI | `http://127.0.0.1:8000/health` | `ai-service/.env`；`.local/logs/ai.*.log` |
| Java 后端 | `http://127.0.0.1:8080/api/v1/health` | `ai-learn-backend/.env`；`.local/logs/backend.*.log` |
| 前端 | `http://127.0.0.1:5173` | `ai-learn-web/.env.local`；`.local/logs/web.*.log` |

本机仅需要 MySQL；Redis、Qdrant 无需启动。3307 是独立本地实例，数据放在 `.local/mysql-data`，腾讯云数据库不会参与本机启动。MySQL 程序沿用此前下载的 `tmp/assistant-verification/mysql-8.4.11-winx64`，请保留该程序目录；如移动，修改 `.local/dev.config.json` 的 `mysqlHome`。

当前本机数据库的 BASIC 档位已更新为 `gpt-6-luna`，模型地址与 Python `.env` 一致，Key 从 Python `.env` 读取；本次配置已完成，不需要额外手工执行模型配置 SQL。以后更换模型时，需要同步修改 Python `.env` 和本地数据库的 BASIC 配置，并重启服务。PRO/SUPER 仍按管理端配置和用户权益生效。本地数据库独立于线上账号，需要本地注册。

真实模型若返回 `402 Insufficient Balance`，说明 Key 对应账户余额不足；补足余额后可继续调用，不需要重建数据库或重新填写本地配置。

下面保留通用安装和分服务启动方式，其他机器可参考这些步骤搭建。

### 1. 本地环境准备

建议本地准备以下运行环境：

| 环境 | 推荐版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | 运行 `ai-learn-backend`。 |
| Maven | 3.9.x 或兼容版本 | 构建和启动 Spring Boot 后端。 |
| Node.js | 20 LTS 或 22 LTS | 运行 `ai-learn-web`。 |
| Python | 3.11+ | 运行 `ai-service`。 |
| MySQL | 8.4 LTS | 保存用户、系统题库、刷题汇总和成长数据。 |

中间件安装、启动和部署注意事项请优先查看：

- [MySQL 本地与部署说明](doc/中间件/MySQL.md)
- [AI模型服务配置说明](doc/中间件/AI模型服务.md)
- [Redis 本地与部署说明](doc/中间件/Redis.md)

说明：基础登录、题库、互动、刷题和成长功能依赖 MySQL；启用 AI 评分/讨论需启动 `ai-service`。Qdrant 预留代码和配置已移除，当前本地开发与生产部署都不需要部署 Qdrant。Redis 当前未接入运行代码，文档仅作为后续缓存/限流能力预留参考。所有真实密码、Token、密钥和生产连接地址都只能保存在本地私有配置中，禁止提交到仓库。

### 2. 准备后端配置

后端读取系统环境变量，建议本地维护 `ai-learn-backend/.env` 作为占位配置来源；使用 IDE 启动时可将这些键值导入运行配置，使用 PowerShell 启动时可先将 `.env` 加载到当前进程环境变量。

`ai-learn-backend/.env` 示例：

```env
DATABASE_URL="jdbc:mysql://127.0.0.1:3306/ai_learn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
DATABASE_USERNAME="本地MySQL用户名占位符"
DATABASE_PASSWORD="本地MySQL密码占位符"
SPRING_FLYWAY_ENABLED="true"
JWT_SECRET="至少32字节本地JWT随机密钥占位符"
JWT_EXPIRES_IN_SECONDS="7200"

# 启用 AI 服务时，请保持 token 与 ai-service/.env 一致。
AI_SERVICE_ENABLED="true"
AI_SERVICE_BASE_URL="http://127.0.0.1:8000"
AI_SERVICE_TOKEN="AI_SERVICE_TOKEN本地占位符"
AI_SERVICE_TIMEOUT_SECONDS="15"

# 内存级限流，生产默认开启；后续如迁移 Redis，需要同步更新中间件文档。
RATE_LIMIT_ENABLED="true"
RATE_LIMIT_LOGIN_LIMIT="10"
RATE_LIMIT_LOGIN_WINDOW_SECONDS="60"
RATE_LIMIT_REGISTER_LIMIT="3"
RATE_LIMIT_REGISTER_WINDOW_SECONDS="3600"
RATE_LIMIT_LIKE_LIMIT="30"
RATE_LIMIT_LIKE_WINDOW_SECONDS="60"
RATE_LIMIT_COMMENT_LIMIT="10"
RATE_LIMIT_COMMENT_WINDOW_SECONDS="60"
RATE_LIMIT_CSV_IMPORT_LIMIT="3"
RATE_LIMIT_CSV_IMPORT_WINDOW_SECONDS="600"
RATE_LIMIT_AI_REQUEST_LIMIT="8"
RATE_LIMIT_AI_REQUEST_WINDOW_SECONDS="60"
RATE_LIMIT_AI_CONCURRENT_LIMIT="1"
```

PowerShell 临时加载 `.env` 示例：

```powershell
cd ai-learn-backend
Get-Content .\.env | Where-Object { $_ -and $_ -notmatch '^\s*#' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim().Trim('"'), 'Process')
}
```

说明：Flyway 默认启用，后端启动后会自动执行 `src/main/resources/db/migration` 下全部数据库迁移，初始化用户、互动、题库、刷题会话、成长徽章、系统设置、JWT 失效记录和超级管理员标识相关表结构。`JWT_SECRET` 必须配置为至少 32 字节的高强度随机值，不能直接使用占位符，否则后端会拒绝启动。当前限流为单机内存级过渡方案，登录和注册按 IP 限流，评论、点赞、CSV 导入和 AI 流式请求同时按 IP 与用户限流，AI 流式请求额外限制单用户并发。

### 3. 准备前端配置

`ai-learn-web/.env` 示例：

```env
VITE_API_BASE_URL=/api/v1
```

### 4. 准备 AI 服务配置

启动 AI 服务前建议配置 `ai-service/.env`：

```env
AI_SERVICE_TOKEN=AI_SERVICE_TOKEN本地占位符
AI_GRADING_BASE_URL=https://模型服务地址占位符/v1
AI_GRADING_API_KEY=AI_GRADING_API_KEY占位符
AI_GRADING_MODEL=LOCAL_RULE
AI_GRADING_MODEL_PROVIDER=
AI_GRADING_TIMEOUT_SECONDS=20
AI_GRADING_MAX_OUTPUT_TOKENS=800
```

说明：`AI_GRADING_MODEL=LOCAL_RULE` 表示使用本地规则兜底能力；如果后续接入真实模型，`AI_GRADING_API_KEY` 必须替换为本地私有值，禁止提交真实密钥。

### 5. 启动顺序

#### 5.1 启动 MySQL

按 [MySQL 本地与部署说明](doc/中间件/MySQL.md) 创建本地数据库和业务账号，确保 `DATABASE_URL`、`DATABASE_USERNAME`、`DATABASE_PASSWORD` 与本地配置一致。

#### 5.2 启动 AI 服务

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

#### 5.3 启动后端

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

#### 5.4 启动前端

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
4. Qdrant 预留代码和配置已移除，当前无需部署 Qdrant。
5. 在 MySQL 中执行 `SHOW TABLES;`，确认 Flyway 已初始化当前迭代所需业务表。

说明：本项目当前要求禁止新增单元测试代码，验收过程只记录构建、接口联调和人工验收步骤。

