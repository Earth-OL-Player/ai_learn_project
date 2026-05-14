## Why

sprint202602 需要在已有页面和工程底座上补齐真实用户体系，解决当前仅有登录注册占位、缺少用户数据持久化和权限引导的问题。该能力是后续建议评论、刷题记录、AI 智能刷题等登录后功能的基础，因此本迭代优先建设注册、登录、当前用户、退出和个人中心基础能力。

## What Changes

- 后端接入 MySQL、Flyway 和用户表初始化能力，使用安全哈希保存密码。
- 新增用户注册接口，支持用户名、密码、昵称、邮箱注册，并返回 JWT access token 与用户摘要。
- 新增用户登录接口，校验用户名密码后返回 JWT access token 与用户摘要。
- 新增当前用户接口 `/api/v1/users/me`，基于 Bearer Token 返回当前登录用户信息。
- 新增退出登录接口 `/api/v1/auth/logout`，采用无状态 JWT 模式，由前端清理 token。
- 前端新增真实登录态管理、登录注册弹窗、刷新后登录态恢复、受保护路由登录引导和个人中心基础页。
- 同步补充或核对 MySQL 中间件说明文档，确保本地安装、启动、配置、验证和服务器部署注意事项完整。

## Capabilities

### New Capabilities
- `user-auth`: 覆盖用户注册、登录、JWT 签发、退出登录、当前用户查询、认证错误处理与安全约束。
- `user-profile`: 覆盖个人中心基础展示，包括头像、用户名、昵称、邮箱、注册时间、经验等级和段位占位信息。

### Modified Capabilities
- `backend-foundation`: sprint202602 后端开始允许并要求接入 MySQL、Flyway、用户表迁移和敏感配置占位说明。
- `frontend-app-shell`: 顶部用户入口从占位升级为真实登录注册入口，并为热门面经、AI智能刷题、个人中心等受保护入口提供登录引导和路由守卫。

## Impact

- 后端模块：`ai-learn-backend` 新增 MySQL、Flyway、MyBatis 或等价持久化依赖，新增 `auth`、`user`、`common/security` 相关包和配置。
- 数据库：新增 `users` 表迁移脚本，用户名唯一，邮箱唯一，密码仅保存哈希。
- API：新增 `/api/v1/auth/register`、`/api/v1/auth/login`、`/api/v1/auth/logout`、`/api/v1/users/me`。
- 前端模块：`ai-learn-web` 新增 auth/user API、Pinia 登录态、登录注册弹窗、登录引导弹窗、个人中心页面和路由守卫。
- 配置与文档：新增或更新数据库连接、JWT 密钥、token 过期时间等占位配置；更新 `doc/中间件/MySQL.md`，不得提交真实密码、Token、密钥或生产连接地址。
