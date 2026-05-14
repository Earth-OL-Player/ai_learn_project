## MODIFIED Requirements

### Requirement: 本期后端不得依赖运行时中间件
系统 SHALL 在 sprint202601 后端运行时不引入 MySQL、Redis、Qdrant 等中间件依赖或连接配置；系统 SHALL 在 sprint202602 起为用户注册登录能力接入 MySQL 与 Flyway，并 SHALL 仅使用占位配置或环境变量声明数据库连接信息。

#### Scenario: 查看 sprint202602 后端配置
- **WHEN** 开发者查看后端配置文件
- **THEN** 配置 SHALL 包含 MySQL 数据源、Flyway、JWT 密钥和 token 过期时间的占位配置，且 SHALL NOT 包含真实密码、Token、密钥或生产连接地址

#### Scenario: 本地未准备 MySQL 时启动后端
- **WHEN** 开发者未按文档准备 MySQL 数据库并启动依赖数据库的后端配置
- **THEN** 后端 MAY 因数据库不可用启动失败，但文档 SHALL 提供明确的本地安装、启动、建库和验证方式

## ADDED Requirements

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
- **THEN** 表 SHALL 对 `username` 建立唯一约束，并 SHALL 对非空 `email` 建立唯一约束

### Requirement: MySQL 中间件文档覆盖本地联调
系统 SHALL 在 `doc/中间件/` 目录提供 MySQL 说明文档，覆盖用途、推荐版本、本地安装方式、本地启动方式、必要配置项、示例占位符配置、验证方式和服务器部署注意事项。

#### Scenario: 查看 MySQL 文档
- **WHEN** 开发者打开 `doc/中间件/MySQL.md`
- **THEN** 文档 SHALL 说明 MySQL 在项目中的用途、推荐版本、本地安装、启动、配置、验证和部署注意事项

#### Scenario: 查看 MySQL 示例配置
- **WHEN** 开发者查看 `doc/中间件/MySQL.md` 中的配置示例
- **THEN** 示例 SHALL 使用占位符展示数据库地址、用户名和密码，并用中文注释说明真实值应如何在本地私有环境配置
