# MySQL 中间件说明

版本：v1.5  
日期：2026-05-16  
适用工程：`ai-learn-backend`  
适用迭代：`sprint202602` 用户注册登录与权限基础、`sprint202603` 建议评论区最小闭环、`sprint202604` 热门面经与默认题库基础、`sprint202611` 超级管理员管理者中心入口

## 1. 用途

MySQL 是项目的业务主库，用于保存用户注册登录数据、建议评论区互动数据、默认题库数据和超级管理员标识，包括用户、建议、评论、题目、知识点、题目知识点关系和审计字段。

当前边界说明：

- `ai-learn-backend` 负责读写 MySQL。
- Flyway 负责自动执行用户、建议、评论、题目和知识点相关表 migration。
- 密码只保存 BCrypt 哈希，禁止保存明文密码。
- 建议状态创建时默认为 `PENDING`，评论点赞和回复字段本期只预留。
- 默认题库通过 migration 初始化少量 AI/RAG/Agent 示例题，供热门面经页面浏览筛选。
- `users.super_admin` 用于标识超级管理员，默认注册用户为普通用户，只允许后台开发者通过数据库维护。
- Redis、Qdrant 等中间件不参与建议评论区最小闭环。

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
DESC suggestions;
DESC comments;
DESC questions;
DESC knowledge_points;
DESC question_knowledge_points;
SHOW INDEX FROM users;
SHOW INDEX FROM suggestions;
SHOW INDEX FROM comments;
SHOW INDEX FROM questions;
SHOW INDEX FROM knowledge_points;
SELECT id, username, super_admin FROM users WHERE username = '本地用户名占位符';
```

后端验证：

1. 设置本地环境变量 `DATABASE_PASSWORD` 和 `JWT_SECRET`。
2. 启动 `ai-learn-backend`。
3. 确认 Flyway 已执行 `V1` 到 `V10` migration，包含用户、互动、题库、刷题、RAG、成长徽章和超级管理员标识相关表结构。
4. 调用 `/api/v1/auth/register` 注册用户。
5. 查询 `users.password_hash`，确认保存的是 BCrypt 哈希而不是明文密码。
6. 调用 `/api/v1/auth/login` 获取 token。
7. 使用 `Authorization: Bearer <accessToken占位符>` 调用 `/api/v1/users/me`。
8. 以游客身份调用 `GET /api/v1/suggestions?pageNo=1&pageSize=10` 和 `GET /api/v1/comments?pageNo=1&pageSize=10`，确认可公开分页查询。
9. 使用 `Authorization: Bearer <accessToken占位符>` 调用 `POST /api/v1/suggestions` 和 `POST /api/v1/comments`，确认登录用户可发布。
10. 查询 `suggestions.status` 默认为 `PENDING`，查询 `comments.like_count` 默认为 `0`。
11. 使用 `Authorization: Bearer <accessToken占位符>` 调用 `GET /api/v1/questions?pageNo=1&pageSize=10`，确认可分页查询默认题库。
12. 调用 `GET /api/v1/knowledge-points`，确认可查询知识点筛选数据。
13. 调用 `GET /api/v1/questions/<题目ID占位符>`，确认可查看题目内容、参考答案、解析和知识点。
14. 注册一个本地普通用户，确认 `users.super_admin` 默认等于 `0`。
15. 如需验收超级管理员入口，可在本地测试库执行 `UPDATE users SET super_admin = 1 WHERE username = '本地用户名占位符';`，重新登录后确认前端展示“管理者中心”。

## 8. 后续部署到服务器注意事项

- 禁止使用 root 账号作为业务账号。
- 生产密码和 `JWT_SECRET` 必须使用高强度随机值，并通过环境变量、服务器私有配置或密钥管理系统注入。
- MySQL 端口不直接暴露公网，只允许应用服务器或可信内网访问。
- 数据目录必须持久化，并制定备份策略，至少包含定期全量备份和恢复演练。
- 上线前确认字符集、时区、排序规则与本地环境一致。
- 表结构变更必须通过 Flyway migration 发布，不允许只在生产库手工改表。
- 生产环境调整超级管理员必须走审批流程，执行 SQL 时只能更新明确账号，禁止批量无条件更新 `users.super_admin`。
- 日志和监控中不得输出完整连接串、用户名密码、JWT token 或敏感业务数据。
