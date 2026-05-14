# user-auth Specification

## Purpose

定义用户注册、登录、退出、当前用户查询、JWT 签发与解析、密码安全存储、认证错误处理、字段唯一性校验和登录态活跃自动续期能力，为后续登录后业务功能提供统一认证基础。

## Requirements

### Requirement: 用户可以注册并获得登录凭证
系统 SHALL 提供 `/api/v1/auth/register` 注册接口，允许游客使用用户名、密码、昵称和邮箱注册账号；注册成功后 SHALL 返回 JWT access token、token 类型、过期时间和用户摘要。

#### Scenario: 注册成功
- **WHEN** 游客提交符合规则且未被占用的 `username`、`password`、必填 `nickname` 和必填 `email`
- **THEN** 系统 SHALL 创建用户、保存密码哈希，并返回 `tokenType=Bearer`、`accessToken`、`expiresIn` 和不包含密码哈希的用户摘要

#### Scenario: 用户名格式不合法
- **WHEN** 游客提交少于 3 位、多于 32 位，或包含非字母、数字、下划线字符的 `username`
- **THEN** 系统 SHALL 拒绝注册并通过统一响应返回参数校验失败信息

#### Scenario: 密码长度不合法
- **WHEN** 游客提交少于 8 位或多于 64 位的 `password`
- **THEN** 系统 SHALL 拒绝注册并通过统一响应返回参数校验失败信息

#### Scenario: 邮箱格式不合法
- **WHEN** 游客提交为空或不符合邮箱格式的 `email`
- **THEN** 系统 SHALL 拒绝注册并通过统一响应返回参数校验失败信息

#### Scenario: 用户名已存在
- **WHEN** 游客使用已存在的 `username` 注册
- **THEN** 系统 SHALL 拒绝注册并返回 `RESOURCE_CONFLICT` 错误码和明确中文提示

#### Scenario: 昵称为空或已存在
- **WHEN** 游客提交空 `nickname` 或使用已存在的 `nickname` 注册
- **THEN** 系统 SHALL 拒绝注册，并在重复时返回 `RESOURCE_CONFLICT` 错误码和明确中文提示

#### Scenario: 邮箱已存在
- **WHEN** 游客使用已存在的 `email` 注册
- **THEN** 系统 SHALL 拒绝注册并返回 `RESOURCE_CONFLICT` 错误码和明确中文提示

### Requirement: 密码必须安全存储
系统 SHALL 使用 BCrypt 或 Argon2 对用户密码进行哈希后保存，系统 SHALL NOT 保存或返回明文密码。

#### Scenario: 注册后查看用户数据
- **WHEN** 用户注册成功后开发者查看 `users` 表中的密码字段
- **THEN** 系统 SHALL 仅保存密码哈希，且该值 SHALL NOT 等于注册时提交的明文密码

#### Scenario: 任意用户接口响应
- **WHEN** 客户端调用注册、登录或当前用户接口成功
- **THEN** 系统 SHALL NOT 在响应体中返回 `password` 或 `passwordHash` 字段

### Requirement: 用户可以登录并获得登录凭证
系统 SHALL 提供 `/api/v1/auth/login` 登录接口，允许游客使用用户名和密码登录；认证成功后 SHALL 返回 JWT access token、token 类型、过期时间和用户摘要。

#### Scenario: 登录成功
- **WHEN** 游客提交存在的 `username` 和正确的 `password`
- **THEN** 系统 SHALL 返回 `tokenType=Bearer`、`accessToken`、`expiresIn` 和不包含密码哈希的用户摘要

#### Scenario: 密码错误
- **WHEN** 游客提交存在的 `username` 和错误的 `password`
- **THEN** 系统 SHALL 拒绝登录并返回 `AUTH_UNAUTHORIZED` 错误码

#### Scenario: 用户名不存在
- **WHEN** 游客提交不存在的 `username` 和任意 `password`
- **THEN** 系统 SHALL 拒绝登录并返回 `AUTH_UNAUTHORIZED` 错误码

### Requirement: 登录用户可以查询当前用户
系统 SHALL 提供 `/api/v1/users/me` 接口，基于请求头 `Authorization: Bearer <token>` 解析当前用户并返回用户摘要。

#### Scenario: 携带有效 token 查询当前用户
- **WHEN** 登录用户携带有效 Bearer token 请求 `/api/v1/users/me`
- **THEN** 系统 SHALL 返回当前用户的 `id`、`username`、`nickname`、`avatar`、`email`、`experience`、`level`、`levelName`、`rank` 和 `createdAt`

#### Scenario: 未携带 token 查询当前用户
- **WHEN** 游客不携带 Bearer token 请求 `/api/v1/users/me`
- **THEN** 系统 SHALL 拒绝访问并返回 `AUTH_UNAUTHORIZED` 错误码

#### Scenario: 携带无效 token 查询当前用户
- **WHEN** 客户端携带格式错误、签名错误或已过期的 Bearer token 请求 `/api/v1/users/me`
- **THEN** 系统 SHALL 拒绝访问并返回 `AUTH_UNAUTHORIZED` 错误码

### Requirement: 用户可以退出登录
系统 SHALL 提供 `/api/v1/auth/logout` 退出登录接口，登录用户请求后系统 SHALL 返回成功，前端 SHALL 清理本地 token 和用户信息。

#### Scenario: 登录用户退出成功
- **WHEN** 登录用户请求 `/api/v1/auth/logout`
- **THEN** 系统 SHALL 返回统一成功响应，且不要求服务端持久化 token 黑名单

### Requirement: JWT 配置必须使用安全占位方式
系统 SHALL 通过环境变量或本地私有配置读取 JWT 密钥与过期时间，示例配置 SHALL 使用占位符，默认过期时间 SHALL 为 7200 秒。

#### Scenario: 查看示例配置
- **WHEN** 开发者查看仓库内提交的后端示例配置
- **THEN** 配置 SHALL 包含 `JWT_SECRET` 和 `JWT_EXPIRES_IN_SECONDS` 占位方式，且 SHALL NOT 包含真实密钥或生产 token

#### Scenario: 登录成功响应过期时间
- **WHEN** 用户注册或登录成功
- **THEN** 响应中的 `expiresIn` SHALL 与后端 JWT 过期配置保持一致


### Requirement: 登录态支持活跃自动续期
系统 SHALL 在客户端携带有效 Bearer token 调用后端接口且 token 校验通过后，通过响应头 `X-Refresh-Token` 返回新 token，以延长活跃用户登录态。

#### Scenario: 后端接口调用后返回续期 token
- **WHEN** 登录用户携带有效 Bearer token 调用后端接口
- **THEN** 系统 SHALL 在响应头 `X-Refresh-Token` 中返回新 access token

#### Scenario: 无效 token 不续期
- **WHEN** 客户端携带无效或已过期 Bearer token 调用受保护接口
- **THEN** 系统 SHALL 拒绝访问并返回 `AUTH_UNAUTHORIZED` 错误码，且 SHALL NOT 返回续期 token
