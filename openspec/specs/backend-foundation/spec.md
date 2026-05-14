# backend-foundation Specification

## Purpose

定义后端工程底座、统一响应结构、请求链路追踪、健康检查、本地启动验证、中间件接入边界以及 MySQL/Flyway 本地联调文档要求，确保后续业务功能具备稳定一致的后端基础能力。

## Requirements

### Requirement: 后端工程可本地启动
系统 SHALL 提供 `ai-learn-backend` 后端工程，并配置 Spring Boot 应用名、服务端口和基础日志配置，以支持本地启动和接口访问。

#### Scenario: 本地启动后端工程
- **WHEN** 开发者按照启动说明启动 `ai-learn-backend`
- **THEN** 后端应用 SHALL 在本地 `8080` 端口或配置端口启动，并暴露本期要求的游客接口

### Requirement: 后端接口使用统一响应结构
系统 SHALL 让后端业务接口返回统一响应结构，包含 `code`、`message`、`data`、`traceId` 字段。

#### Scenario: 接口成功响应
- **WHEN** 游客请求后端成功的业务接口
- **THEN** 系统 SHALL 返回包含 `code=SUCCESS`、中文成功 `message`、业务 `data` 和 `traceId` 的响应体

#### Scenario: 接口异常响应
- **WHEN** 后端处理请求时发生可预期业务异常或系统异常
- **THEN** 系统 SHALL 返回统一响应结构，并包含可读中文 `message` 和 `traceId`

### Requirement: 请求链路包含 traceId
系统 SHALL 为请求生成或传递 `traceId`，并在统一响应中返回该 `traceId`，用于本地联调和问题定位。

#### Scenario: 请求未携带 traceId
- **WHEN** 客户端请求未携带 traceId
- **THEN** 系统 SHALL 生成新的 traceId，并在响应体中返回

#### Scenario: 请求携带 traceId
- **WHEN** 客户端请求携带可用 traceId
- **THEN** 系统 SHALL 优先沿用该 traceId 或保持响应 traceId 与当前请求链路一致

### Requirement: 后端提供健康检查
系统 SHALL 提供游客可访问的健康检查能力，至少包含 `/api/v1/health`，并可兼容 `/actuator/health`。

#### Scenario: 访问健康检查接口
- **WHEN** 游客请求健康检查端点
- **THEN** 系统 SHALL 返回应用可用状态，且不要求登录鉴权

### Requirement: 本期后端不得依赖运行时中间件
系统 SHALL 在 sprint202601 后端运行时不引入 MySQL、Redis、Qdrant 等中间件依赖或连接配置；系统 SHALL 在 sprint202602 起为用户注册登录能力接入 MySQL 与 Flyway，并 SHALL 仅使用占位配置或环境变量声明数据库连接信息。

#### Scenario: 查看 sprint202602 后端配置
- **WHEN** 开发者查看后端配置文件
- **THEN** 配置 SHALL 包含 MySQL 数据源、Flyway、JWT 密钥和 token 过期时间的占位配置，且 SHALL NOT 包含真实密码、Token、密钥或生产连接地址

#### Scenario: 本地未准备 MySQL 时启动后端
- **WHEN** 开发者未按文档准备 MySQL 数据库并启动依赖数据库的后端配置
- **THEN** 后端 MAY 因数据库不可用启动失败，但文档 SHALL 提供明确的本地安装、启动、建库和验证方式

### Requirement: 启动说明覆盖本地验证
系统 SHALL 提供 README 或启动说明，包含前后端本地启动命令、接口验证方式、构建验证方式和人工验收清单。

#### Scenario: 按文档完成本地验证
- **WHEN** 开发者按照启动说明进行本地验证
- **THEN** 开发者 SHALL 能完成后端健康检查、前端页面访问和菜单切换验收


### Requirement: 后端使用 Flyway 初始化用户表
系统 SHALL 使用 Flyway 管理用户表迁移脚本，并在本地空数据库首次启动时创建 `users` 表。

#### Scenario: 空数据库首次启动后端
- **WHEN** 开发者创建本地 `ai_learn` 数据库并启动后端
- **THEN** Flyway SHALL 自动执行用户表迁移脚本并创建 `users` 表

#### Scenario: 查看用户表结构
- **WHEN** 开发者查看 `users` 表结构
- **THEN** 表 SHALL 包含用户 ID、用户名、昵称、头像、邮箱、密码哈希、经验值、等级编码、段位编码、创建时间、更新时间和逻辑删除标识字段

#### Scenario: 用户唯一约束
- **WHEN** 开发者查看 `users` 表索引
- **THEN** 表 SHALL 对 `username`、`nickname` 和 `email` 建立唯一约束

### Requirement: MySQL 中间件文档覆盖本地联调
系统 SHALL 在 `doc/中间件/` 目录提供 MySQL 说明文档，覆盖用途、推荐版本、本地安装方式、本地启动方式、必要配置项、示例占位符配置、验证方式和服务器部署注意事项。

#### Scenario: 查看 MySQL 文档
- **WHEN** 开发者打开 `doc/中间件/MySQL.md`
- **THEN** 文档 SHALL 说明 MySQL 在项目中的用途、推荐版本、本地安装、启动、配置、验证和部署注意事项

#### Scenario: 查看 MySQL 示例配置
- **WHEN** 开发者查看 `doc/中间件/MySQL.md` 中的配置示例
- **THEN** 示例 SHALL 使用占位符展示数据库地址、用户名和密码，并用中文注释说明真实值应如何在本地私有环境配置
