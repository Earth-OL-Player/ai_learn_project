# backend-foundation Specification

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
系统 SHALL NOT 在 sprint202601 后端运行时中引入 MySQL、Redis、Qdrant 等中间件依赖或连接配置。

#### Scenario: 查看后端配置
- **WHEN** 开发者查看后端配置文件
- **THEN** 配置 SHALL 仅包含服务端口、应用名、日志等基础配置，不包含数据库、缓存、向量库连接地址或真实敏感信息

### Requirement: 启动说明覆盖本地验证
系统 SHALL 提供 README 或启动说明，包含前后端本地启动命令、接口验证方式、构建验证方式和人工验收清单。

#### Scenario: 按文档完成本地验证
- **WHEN** 开发者按照启动说明进行本地验证
- **THEN** 开发者 SHALL 能完成后端健康检查、前端页面访问和菜单切换验收
