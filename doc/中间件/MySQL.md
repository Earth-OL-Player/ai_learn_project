# MySQL 中间件说明

版本：v1.1  
日期：2026-05-14  
适用工程：`ai-learn-backend`

## 1. 用途

MySQL 是项目的业务主库，保存用户、建议、评论、题库、答题记录、成长体系、徽章和刷题会话等核心数据。

边界说明：

- `ai-learn-backend` 负责读写 MySQL。
- `ai-service` 不直接写入 MySQL 核心业务表。
- AI 生成的评分、建议或题目，需要先经过后端校验，再由后端落库。

## 2. 推荐版本

推荐使用：

```text
MySQL 8.4 LTS
```

选择理由：

- LTS 版本适合长期维护。
- 本地和生产保持同一主版本，减少 SQL 行为差异。
- JDBC 驱动使用与 MySQL 8.4 LTS 兼容的 MySQL Connector/J 稳定版本。

## 3. 本地安装方式

### 3.1 Docker 方式（推荐）

```bash
docker run --name ai-learn-mysql \
  -e MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD} \
  -e MYSQL_DATABASE=ai_learn \
  -e MYSQL_USER=${DATABASE_USERNAME} \
  -e MYSQL_PASSWORD=${DATABASE_PASSWORD} \
  -p 3306:3306 \
  -v ai-learn-mysql-data:/var/lib/mysql \
  -d mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_0900_ai_ci
```

占位符说明：

- `${MYSQL_ROOT_PASSWORD}`：本地 root 密码，由开发者自行设置，不提交仓库。
- `${DATABASE_USERNAME}`：业务库用户名，例如本地可设置为 `ai_learn_user`。
- `${DATABASE_PASSWORD}`：业务库密码，由开发者自行设置，不提交仓库。

### 3.2 本机安装方式

也可以通过 MySQL 官方安装包、本机包管理器或开发环境工具安装 MySQL 8.4 LTS。

安装后需要完成：

- 创建业务库：`ai_learn`。
- 创建本地业务账号。
- 字符集使用 `utf8mb4`。
- 默认端口使用 `3306`；如修改端口，同步调整 `DATABASE_URL`。

## 4. 本地启动方式

启动容器：

```bash
docker start ai-learn-mysql
```

停止容器：

```bash
docker stop ai-learn-mysql
```

查看日志：

```bash
docker logs -f ai-learn-mysql
```

## 5. 必要配置项

`ai-learn-backend` 需要以下配置：

| 配置项 | 示例占位符 | 说明 |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:mysql://127.0.0.1:3306/ai_learn` | MySQL JDBC 地址 |
| `DATABASE_USERNAME` | `${DATABASE_USERNAME}` | 业务库账号 |
| `DATABASE_PASSWORD` | `${DATABASE_PASSWORD}` | 业务库密码 |
| `SPRING_FLYWAY_ENABLED` | `true` | 是否启用 Flyway migration |

## 6. 示例配置

`application-local.example.yml`：

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://127.0.0.1:3306/ai_learn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    username: ${DATABASE_USERNAME:ai_learn_user}
    password: ${DATABASE_PASSWORD:请在本地私有配置中填写真实密码}
  flyway:
    enabled: ${SPRING_FLYWAY_ENABLED:true}
    locations: classpath:db/migration
```

注意：

- 示例密码是占位符，不是真实密码。
- 真实密码只能放在本地私有配置、环境变量或服务器私有配置中。
- `application-local.yml`、`.env` 等真实配置文件必须加入 `.gitignore`。

## 7. 验证方式

命令行连接：

```bash
mysql -h 127.0.0.1 -P 3306 -u ${DATABASE_USERNAME} -p
```

SQL 验证：

```sql
SELECT VERSION();
SHOW DATABASES;
USE ai_learn;
SHOW TABLES;
```

后端验证：

- 启动 `ai-learn-backend`。
- 确认 Flyway migration 执行成功。
- 访问后端健康检查接口，例如 `/actuator/health`。
- 执行注册、登录或查询接口，确认能正常读写业务数据。

## 8. 生产部署注意事项

- 禁止使用 root 账号作为业务账号。
- 生产密码必须使用高强度随机值，并通过环境变量或服务器私有配置注入。
- MySQL 端口不直接暴露公网，只允许应用服务器或可信内网访问。
- 数据目录必须持久化。
- 必须制定备份策略，至少包含定期全量备份和恢复演练。
- 上线前确认字符集、时区、排序规则与本地环境一致。
- 表结构变更必须通过 Flyway migration 发布，不允许只在生产库手工改表。
- 日志和监控中不得输出完整连接串、用户名密码或敏感业务数据。
