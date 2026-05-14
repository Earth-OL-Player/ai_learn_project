# AI 学习项目

本项目当前迭代为 `sprint202602`，目标是在已有前后端工程底座上补充用户注册、登录、退出、当前用户、基础权限引导和个人中心基础展示能力。学习路线页面继续使用前端项目内 Markdown 文件静态渲染。

## 工程结构

```text
ai_learn_project/
├── ai-learn-backend/   # Spring Boot 后端工程
├── ai-learn-web/       # Vue 3 前端工程
├── doc/                # 需求、设计、迭代和中间件文档
└── openspec/           # OpenSpec 变更说明
```

## 后端本地启动

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.4 LTS（本期用户体系依赖）

### MySQL 准备

推荐使用 Docker 启动本地 MySQL：

```powershell
docker run --name ai-learn-mysql `
  -e MYSQL_ROOT_PASSWORD="本地root密码占位符" `
  -e MYSQL_DATABASE="ai_learn" `
  -e MYSQL_USER="ai_learn_user" `
  -e MYSQL_PASSWORD="本地业务用户密码占位符" `
  -p 3306:3306 `
  -v ai-learn-mysql-data:/var/lib/mysql `
  -d mysql:8.4 `
  --character-set-server=utf8mb4 `
  --collation-server=utf8mb4_0900_ai_ci
```

详细说明见：`doc/中间件/MySQL.md`。

### 必要环境变量

PowerShell 示例：

```powershell
$env:DATABASE_URL="jdbc:mysql://127.0.0.1:3306/ai_learn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:DATABASE_USERNAME="ai_learn_user"
$env:DATABASE_PASSWORD="本地业务用户密码占位符"
$env:JWT_SECRET="至少32位JWT密钥占位符请替换为本地真实值"
$env:JWT_EXPIRES_IN_SECONDS="7200"
```

注意：以上值均为本地开发占位示例，真实密码和 JWT 密钥禁止提交仓库。

### 启动命令

```powershell
cd ai-learn-backend
mvn spring-boot:run
```

后端默认启动地址：

```text
http://localhost:8080
```

首次连接空数据库时，Flyway 会自动执行：

```text
ai-learn-backend/src/main/resources/db/migration/V1__init_user_tables.sql
```

## 前端本地启动

### 环境要求

- Node.js 20+
- npm 10+

### 环境变量

复制示例配置：

```powershell
cd ai-learn-web
Copy-Item .env.example .env
```

`.env.example` 内容如下：

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

该地址仅为本地开发占位值。部署到其他环境时，请通过环境变量替换为实际后端 API 地址，禁止提交真实生产连接地址或密钥。

### 启动命令

```powershell
cd ai-learn-web
npm install
npm run dev
```

前端默认启动地址：

```text
http://localhost:5173
```

## 学习路线 Markdown 维护方式

学习路线页面直接渲染前端项目内的 Markdown 文件：

```text
ai-learn-web/src/content/learning-roadmap/AI应用开发学习路线和资料集.md
```

图片资源目录：

```text
ai-learn-web/src/content/learning-roadmap/AI应用开发学习路线和资料集.assets/
```

维护规则：

- 页面不调用后端学习路线接口获取内容。
- 需要调整页面文案时，直接修改上述 Markdown 文件。
- 如果 Markdown 中新增相对路径图片，需要放到同级 `.assets` 目录中。
- 开发环境下保存 Markdown 后，Vite 会自动刷新页面。
- 不要修改原始 Markdown 内容语义，页面仅负责清新简约地渲染文档。
- 页面左侧展示“目录”，点击目录可跳转到对应标题，当前阅读章节会高亮标识。
- 学习路线页面左侧目录支持收起和展开，收起后正文区域会获得更大展示空间。
- Markdown 图片会根据图片替代文本自动展示图注，例如 `![AI应用开发学习路线](...)` 会展示为“图1-AI应用开发学习路线”。

## 接口联调验证

### 健康检查

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health" -Method Get
```

预期结果：

- `code` 为 `SUCCESS`
- `data.status` 为 `UP`
- 响应包含 `traceId`

### 注册

```powershell
$registerBody = @{
  username = "demo_user"
  password = "demo_password_123"
  nickname = "演示用户"
  email = "demo@example.com"
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $registerBody
```

### 登录

```powershell
$loginBody = @{
  username = "demo_user"
  password = "demo_password_123"
} | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $loginResp.data.accessToken
```

### 当前用户

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/me" -Method Get -Headers @{ Authorization = "Bearer $token" }
```

### 退出登录

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/logout" -Method Post -Headers @{ Authorization = "Bearer $token" }
```

## 构建验证

### 后端构建

```powershell
cd ai-learn-backend
mvn -DskipTests package
```

### 前端构建

```powershell
cd ai-learn-web
npm run build
```

本项目当前要求禁止新增单元测试代码，因此本期只执行构建、接口联调和人工验收。

## 人工验收清单

- 可以完成注册，数据库中保存密码哈希而不是明文。
- 重复用户名注册失败并给出明确中文提示。
- 正确密码可登录，错误密码登录失败。
- 登录成功后右上角展示用户信息。
- 刷新页面后前端能通过 `/users/me` 恢复登录态。
- 游客访问热门面经、AI智能刷题、个人中心时看到“登录后即可使用该功能”。
- 登录用户可以访问个人中心基础页。
- 退出登录后 token 和用户信息被清理。
- 学习路线和建议评论区占位页保持游客可访问。

## 中间件说明

本期新增 MySQL 运行时依赖，已更新：

```text
doc/中间件/MySQL.md
```

该文档覆盖 MySQL 在项目中的用途、推荐版本、本地安装方式、本地启动方式、必要配置项、示例占位符配置、验证方式，以及后续部署到服务器时的注意事项。
