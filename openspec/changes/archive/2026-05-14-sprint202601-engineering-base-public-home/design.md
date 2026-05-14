## Context

当前仓库只有需求、架构和迭代文档，尚未包含可运行的前端或后端工程。sprint202601 的定位是“工程底座与公开首页”，需要在不引入注册登录、数据库和其他中间件的前提下，交付一套可本地启动、可导航、可演示的最小前后端分离应用。

本期约束包括：源码、文档和配置统一使用 UTF-8；禁止新增单元测试代码；不提交真实密码、Token、密钥和生产连接地址；前端 UI 风格保持清新、简约、大气；学习路线数据不得运行时依赖 `doc/` 目录，避免部署后路径不可用。

## Goals / Non-Goals

**Goals:**

- 创建 `ai-learn-web` 前端工程，提供 Vue 3、Vite、TypeScript、Vue Router 和 Element Plus 基础能力。
- 创建 `ai-learn-backend` 后端工程，提供 Spring Boot 基础能力、统一响应、traceId、健康检查和学习路线接口。
- 浏览器访问前端默认进入“AI 学习路线和资料”页面。
- 左侧菜单展示四个规划入口，并支持学习路线页面与三个占位页面之间切换。
- 右上角展示“登录 / 注册”入口占位，点击后以中文提示后续迭代开放。
- README 或启动说明包含前后端本地启动、接口联调、构建验证和人工验收清单。

**Non-Goals:**

- 不实现注册、登录、退出、JWT、权限守卫和个人中心。
- 不连接 MySQL、Redis、Qdrant 或其他中间件。
- 不实现建议、评论、面试题、刷题 Agent 的真实业务接口。
- 不新增单元测试代码。
- 不为后续功能做复杂抽象或提前建设未使用模块。

## Decisions

### 前端采用独立 `ai-learn-web` 工程

选择 Vue 3 + Vite + TypeScript + Vue Router + Element Plus，原因是该组合启动快、配置轻、类型约束明确，适合快速交付可演示的学习平台公开首页。相较于直接使用纯静态页面，该方案能提前沉淀路由、组件和 API 调用边界；相较于引入重型前端框架，本期复杂度更低。

### 前端使用应用壳布局承载所有页面

使用 `layout/AppLayout.vue` 提供左侧菜单、顶部用户区和主内容区，路由页面只关注业务内容。这样可以保证四个规划菜单的视觉和交互一致，也便于后续迭代逐步替换占位页。占位页统一复用 `PlaceholderPage.vue`，避免重复代码。

### 前端 API 基础地址通过 `.env.example` 暴露

新增 `VITE_API_BASE_URL=http://localhost:8080/api/v1` 作为本地占位配置。真实环境值由部署环境注入，不提交生产地址。HTTP 封装统一处理 `code = SUCCESS` 的数据提取、业务失败提示和网络异常提示，页面层只关注展示状态。

### 后端采用独立 `ai-learn-backend` 工程

选择 Spring Boot 提供 REST 接口、统一响应和健康检查。后端包结构按 `common` 与 `learning` 划分，保持简单可读：`common` 承载响应、异常和 trace 相关能力，`learning` 承载学习路线查询接口和应用服务。

### 学习路线数据使用应用内静态结构

学习路线内容来自 `doc/学习路线页面` 的整理版，但运行时通过代码内静态 DTO 或 `resources/learning/roadmap.json` 提供，避免后端部署后依赖仓库文档路径。若使用 JSON 资源文件，应随应用打包并通过类路径读取。

### 统一响应包含 traceId

所有业务接口返回 `code`、`message`、`data`、`traceId`。traceId 可由请求过滤器生成并写入请求上下文，响应构造时读取，便于本地联调和后续问题定位。本期不引入复杂链路追踪依赖。

### 健康检查保持轻量

至少提供游客可访问的 `/api/v1/health`，可按工程依赖情况兼容 Spring Boot Actuator 的 `/actuator/health`。由于本期无数据库和缓存依赖，健康检查只验证应用进程可用。

## Risks / Trade-offs

- [Risk] 学习路线内容静态维护，后续更新需要重新发版 → Mitigation：本期先保证可演示与可部署，后续如引入后台配置再通过独立需求设计数据模型。
- [Risk] 占位菜单可能被误解为已具备真实业务能力 → Mitigation：占位页标题和说明必须明确标注后续迭代开放，不调用不存在的后端接口。
- [Risk] 前后端联调时 API 地址不一致导致页面异常 → Mitigation：提供 `.env.example` 和 README 启动说明，明确本地后端端口与 API 前缀。
- [Risk] 过早抽象导致工程复杂度上升 → Mitigation：仅抽取统一响应、HTTP 封装、布局和占位页等当前确实复用的基础能力。
- [Risk] 未引入中间件可能限制后续真实业务能力 → Mitigation：本期明确不做持久化和权限，后续 sprint202602 再按真实登录需求补充中间件文档与配置。

## Migration Plan

1. 在当前仓库新增 `ai-learn-backend` 与 `ai-learn-web` 两个工程目录。
2. 先实现后端基础接口，确保 `/api/v1/health` 与 `/api/v1/learning/roadmap` 可本地访问。
3. 再实现前端布局、路由和学习路线页面，使用 `.env.example` 指向本地后端。
4. 补充 README 或启动说明，记录本地启动、构建、接口验证和人工验收方式。
5. 如需回滚，删除本次新增工程目录和相关启动说明即可；本期不涉及数据迁移。

## Open Questions

- 学习路线内容最终采用代码内静态 DTO 还是 `resources/learning/roadmap.json`，实现时可优先选择更便于维护且不增加运行时复杂度的方案。
- 健康检查是否同时启用 Actuator，可在实现时根据依赖复杂度决定；最低要求是提供 `/api/v1/health`。
