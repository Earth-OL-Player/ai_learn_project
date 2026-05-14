## Why

本迭代需要先交付可本地启动、可导航、可演示的最小工程底座，为后续登录、评论、题库和刷题 Agent 等能力提供稳定承载页面与接口基础。当前阶段聚焦公开首页与学习路线展示，避免过早引入注册登录、数据库和其他中间件复杂度。

## What Changes

- 新增前端工程 `ai-learn-web`，使用 Vue 3、Vite、TypeScript、Vue Router、Element Plus 搭建清新简约的公开页面骨架。
- 新增后端工程 `ai-learn-backend`，使用 Spring Boot 提供统一响应、traceId、健康检查和学习路线查询接口。
- 新增全局布局：左侧四个规划菜单、顶部登录/注册占位、主内容路由区域。
- 新增“AI 学习路线与资料”公开首页，展示平台说明、阶段路线、资料区和学习建议。
- 新增建议与评论、面试题大全、刷题 Agent 三个占位页面，点击菜单时展示后续迭代说明。
- 新增前后端本地启动与人工验收说明。
- 不引入 MySQL、Redis、Qdrant 等运行时中间件，不实现真实登录和权限守卫。

## Capabilities

### New Capabilities
- `frontend-app-shell`: 覆盖前端工程初始化、路由、全局布局、菜单导航、顶部用户入口占位和占位页面行为。
- `learning-roadmap`: 覆盖学习路线公开首页内容展示，以及后端学习路线结构化查询接口。
- `backend-foundation`: 覆盖后端工程初始化、统一响应、traceId 和健康检查能力。

### Modified Capabilities

无。

## Impact

- 影响新增目录：`ai-learn-web/`、`ai-learn-backend/`。
- 影响接口：新增 `/api/v1/learning/roadmap`，新增 `/api/v1/health` 或兼容健康检查端点。
- 影响配置：新增前端 `.env.example`，提供 `VITE_API_BASE_URL` 本地占位值；后端仅保留应用名、端口和日志等基础配置。
- 影响文档：新增或更新 README/启动说明，补充前后端本地启动、接口联调和人工验收清单。
- 依赖约束：本期不得新增 MySQL、Redis、Qdrant 等中间件运行时依赖，也不得提交真实密码、Token、密钥或生产连接地址。
