# AI 学习项目

本项目当前迭代为 `sprint202601`，目标是交付“能打开、能导航、能看学习路线”的最小工程底座与公开首页。学习路线页面使用前端项目内 Markdown 文件静态渲染，不依赖后端接口获取内容。

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

### 启动命令

```powershell
cd ai-learn-backend
mvn spring-boot:run
```

后端默认启动地址：

```text
http://localhost:8080
```

### 后端基础配置

后端配置文件：`ai-learn-backend/src/main/resources/application.yml`

```yaml
server:
  port: 8080
spring:
  application:
    name: ai-learn-backend
```

本期后端不配置 MySQL、Redis、Qdrant 等中间件连接，不需要真实账号、密码、Token 或生产地址。

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
- 面向用户的页面不展示“本地 Markdown 静态页面”“直接修改前端 Markdown 文件即可同步页面内容”等维护提示。

## 接口联调验证

### 健康检查

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health" -Method Get
```

预期结果：

- `code` 为 `SUCCESS`
- `data.status` 为 `UP`
- 响应包含 `traceId`

### 学习路线接口

当前学习路线前端页面不再依赖该接口获取内容，页面内容来自前端 Markdown 文件。后端接口可作为兼容能力保留，非本次页面验收必需项。

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

- 打开前端根路径 `/` 后，默认进入 `/learning-roadmap`。
- 左侧展示四个菜单：AI 学习路线与资料、建议与评论、面试题大全、刷题 Agent。
- 当前菜单高亮正确，点击菜单可以正常切换页面，顶部不展示“当前页面 XXXXX”标题区。
- 右上角展示“登录 / 注册”入口，点击提示登录能力将在 `sprint202602` 开放。
- 学习路线页面按前端 Markdown 原文渲染，展示 `AI应用开发学习路线和资料集.md` 中的表格、标题、图片、引用、链接和正文内容，并在内容区左侧展示文档目录。
- 建议与评论、面试题大全、刷题 Agent 均展示占位说明，不出现接口报错或空白页。
- 仓库未新增 MySQL、Redis、Qdrant 等运行时中间件依赖。

## 中间件说明

本期不引入 MySQL、Redis、Qdrant 等运行时中间件，因此未新增或更新 `doc/中间件/` 下的文档。

如果后续功能开始依赖中间件，必须同步更新 `doc/中间件/` 对应说明文档，说明用途、推荐版本、本地安装与启动方式、必要配置、示例占位符配置、验证方式和部署注意事项。


补充说明：学习路线页面左侧目录支持收起和展开，收起后正文区域会获得更大展示空间。


补充说明：Markdown 图片会根据图片替代文本自动展示图注，例如 `![AI应用开发学习路线](...)` 会展示为“图1-AI应用开发学习路线”。
