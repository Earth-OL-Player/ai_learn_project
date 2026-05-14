# Redis 中间件说明

版本：v1.1  
日期：2026-05-14  
适用工程：`ai-learn-backend`

## 1. 用途

Redis 用于保存短期、高频、可重建的数据，例如缓存、限流计数、验证码、登录态辅助信息、热点题目和排行榜预留数据。

边界说明：

- Redis 不作为业务主库。
- 用户、题库、答题记录和成长数据以 MySQL 为准。
- Redis 中不保存明文密码、完整 Token、API Key 等敏感信息。

## 2. 推荐版本

推荐使用：

```text
Redis Open Source 8.x 稳定版
```

说明：

- 本地和生产尽量保持同一主版本。
- 如果生产使用云 Redis，需要确认命令兼容性、访问控制和持久化策略。

## 3. 本地安装方式

### 3.1 Docker 方式（推荐）

本地无密码示例：

```bash
docker run --name ai-learn-redis \
  -p 6379:6379 \
  -v ai-learn-redis-data:/data \
  -d redis:8
```

本地带密码示例：

```bash
docker run --name ai-learn-redis \
  -p 6379:6379 \
  -v ai-learn-redis-data:/data \
  -d redis:8 redis-server --requirepass ${REDIS_PASSWORD}
```

占位符说明：

- `${REDIS_PASSWORD}`：本地 Redis 密码，由开发者自行设置，不提交仓库。

### 3.2 本机安装方式

可通过 Redis 官方包、WSL、Linux 包管理器或开发环境工具安装 Redis。

要求：

- 默认端口使用 `6379`。
- 如开启密码，同步配置 `REDIS_PASSWORD`。
- Windows 本机开发优先使用 Docker 或 WSL，减少兼容问题。

## 4. 本地启动方式

启动容器：

```bash
docker start ai-learn-redis
```

停止容器：

```bash
docker stop ai-learn-redis
```

查看日志：

```bash
docker logs -f ai-learn-redis
```

## 5. 必要配置项

`ai-learn-backend` 需要以下配置：

| 配置项 | 示例占位符 | 说明 |
| --- | --- | --- |
| `REDIS_URL` | `redis://127.0.0.1:6379` | Redis 连接地址 |
| `REDIS_PASSWORD` | `${REDIS_PASSWORD}` | Redis 密码，本地无密码时可为空 |
| `REDIS_DATABASE` | `0` | Redis 逻辑库编号 |

## 6. 示例配置

`application-local.example.yml`：

```yaml
spring:
  data:
    redis:
      url: ${REDIS_URL:redis://127.0.0.1:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
```

注意：

- 本地无密码时 `REDIS_PASSWORD` 可以为空。
- 生产环境建议配置密码、内网访问、安全组或云 Redis 白名单。
- 真实密码不得提交仓库。

## 7. 验证方式

无密码：

```bash
redis-cli -h 127.0.0.1 -p 6379 ping
```

带密码：

```bash
redis-cli -h 127.0.0.1 -p 6379 -a ${REDIS_PASSWORD} ping
```

期望返回：

```text
PONG
```

读写验证：

```bash
redis-cli set ai-learn:test ok
redis-cli get ai-learn:test
redis-cli del ai-learn:test
```

后端验证：

- 启动 `ai-learn-backend`。
- 访问登录、验证码、限流或热点缓存相关接口。
- 检查 Redis 中是否出现 `ai-learn:` 前缀的 key。

## 8. 生产部署注意事项

- Redis 端口不直接暴露公网。
- 生产环境必须配置访问控制，例如密码、安全组、内网访问或云 Redis 白名单。
- 重要业务数据不要只保存在 Redis。
- 根据场景选择 RDB、AOF 或云 Redis 托管持久化能力。
- 缓存 key 必须设置合理过期时间，避免无限增长。
- key 命名使用业务前缀，例如 `ai-learn:auth:`、`ai-learn:rate-limit:`、`ai-learn:cache:`。
- 不在 Redis 中保存明文密码、完整 Token、API Key、用户隐私等敏感信息。
