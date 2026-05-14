# MySQL 中间件说明

版本：v1.2  
日期：2026-05-14  
适用工程：`ai-learn-backend`  
适用迭代：`sprint202602` 用户注册登录与权限基础

## 1. 用途

MySQL 是项目的业务主库，本迭代用于保存用户注册登录相关数据，包括用户名、昵称、邮箱、密码哈希、头像占位、经验值、等级、段位和审计字段。

当前边界说明：

- `ai-learn-backend` 负责读写 MySQL。
- Flyway 负责自动执行用户表 migration。
- 密码只保存 BCrypt 哈希，禁止保存明文密码。
- Redis、Qdrant 等中间件本期不接入。

## 2. 推荐版本

推荐使用：

```text
MySQL 8.4 LTS
```

选择理由：

- LTS 版本适合长期维护。
- 本地和服务器保持同一主版本，减少 SQL 行为差异。
- Spring Boot 使用 MySQL Connector/J 连接 MySQL 8.x。

## 3. 本地安装方式

### 3.1 Docker 方式（推荐）

PowerShell 示例：

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

占位符说明：

- `本地root密码占位符`：本地 root 密码，由开发者自行设置，禁止提交仓库。
- `ai_learn_user`：本地业务库用户名，可按团队规范调整。
- `本地业务用户密码占位符`：本地业务库密码，由开发者自行设置，禁止提交仓库。

### 3.2 本机安装方式

也可以通过 MySQL 官方安装包、本机包管理器或开发环境工具安装 MySQL 8.4 LTS。

安装后需要完成：

- 创建业务库：`ai_learn`。
- 创建本地业务账号，例如 `ai_learn_user`。
- 字符集使用 `utf8mb4`。
- 默认端口使用 `3306`；如修改端口，同步调整 `DATABASE_URL`。

## 4. 本地启动方式

启动容器：

```powershell
docker start ai-learn-mysql
```

停止容器：

```powershell
docker stop ai-learn-mysql
```

查看日志：

```powershell
docker logs -f ai-learn-mysql
```

## 5. 必要配置项

`ai-learn-backend` 需要以下配置：

| 配置项 | 示例占位符 | 说明 |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:mysql://127.0.0.1:3306/ai_learn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false` | MySQL JDBC 地址 |
| `DATABASE_USERNAME` | `ai_learn_user` | 业务库账号，按本地实际值配置 |
| `DATABASE_PASSWORD` | `本地业务用户密码占位符` | 业务库密码，真实值只能放在本地环境变量或私有配置中 |
| `SPRING_FLYWAY_ENABLED` | `true` | 是否启用 Flyway migration |
| `JWT_SECRET` | `至少32位JWT密钥占位符` | JWT 签名密钥，真实值禁止提交仓库 |
| `JWT_EXPIRES_IN_SECONDS` | `7200` | access token 过期秒数 |

## 6. 示例占位符配置

后端已在 `ai-learn-backend/src/main/resources/application.yml` 中提供环境变量占位配置：

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://127.0.0.1:3306/ai_learn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}
    username: ${DATABASE_USERNAME:ai_learn_user}
    password: ${DATABASE_PASSWORD:请在本地环境变量中填写真实密码}
  flyway:
    enabled: ${SPRING_FLYWAY_ENABLED:true}
    locations: classpath:db/migration
app:
  jwt:
    secret: ${JWT_SECRET:请在本地环境变量中配置至少32位JWT密钥占位符}
    expires-in-seconds: ${JWT_EXPIRES_IN_SECONDS:7200}
```

注意：

- 示例密码和 JWT 密钥是占位符，不是真实值。
- 真实密码、JWT 密钥只能放在本地环境变量、私有配置或服务器密钥管理系统中。
- `application-local.yml`、`.env` 等真实配置文件必须加入 `.gitignore`。

## 7. 验证方式

命令行连接：

```powershell
mysql -h 127.0.0.1 -P 3306 -u ai_learn_user -p
```

SQL 验证：

```sql
SELECT VERSION();
USE ai_learn;
SHOW TABLES;
DESC users;
SHOW INDEX FROM users;
```

后端验证：

1. 设置本地环境变量 `DATABASE_PASSWORD` 和 `JWT_SECRET`。
2. 启动 `ai-learn-backend`。
3. 确认 Flyway 执行 `V1__init_user_tables.sql` 并创建 `users` 表。
4. 调用 `/api/v1/auth/register` 注册用户。
5. 查询 `users.password_hash`，确认保存的是 BCrypt 哈希而不是明文密码。
6. 调用 `/api/v1/auth/login` 获取 token。
7. 使用 `Authorization: Bearer <accessToken占位符>` 调用 `/api/v1/users/me`。

## 8. 后续部署到服务器注意事项

- 禁止使用 root 账号作为业务账号。
- 生产密码和 `JWT_SECRET` 必须使用高强度随机值，并通过环境变量、服务器私有配置或密钥管理系统注入。
- MySQL 端口不直接暴露公网，只允许应用服务器或可信内网访问。
- 数据目录必须持久化，并制定备份策略，至少包含定期全量备份和恢复演练。
- 上线前确认字符集、时区、排序规则与本地环境一致。
- 表结构变更必须通过 Flyway migration 发布，不允许只在生产库手工改表。
- 日志和监控中不得输出完整连接串、用户名密码、JWT token 或敏感业务数据。
