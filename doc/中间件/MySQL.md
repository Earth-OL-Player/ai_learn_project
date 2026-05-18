# MySQL 中间件说明

版本：v1.13
日期：2026-05-18  
适用工程：`ai-learn-backend`  
适用迭代：`sprint202602` 用户注册登录与权限基础、`sprint202603` 建议评论区最小闭环、`sprint202604` 热门面经与默认题库基础、`sprint202611` 超级管理员管理者中心入口、`sprint202612` 系统题库管理与AI智能刷题重构、`sprint2616` 答题上下文记忆优化、`sprint2617` 成长等级与修仙境界段位重构、`sprint2619` 建议评论区评论流重构、`sprint2620` 刷题勋章强联动、`sprint2621` 用户管理与容量限制

## 1. 用途

MySQL 是项目的业务主库，用于保存用户注册登录数据、建议评论区互动数据、真实 AI 面试题库数据和超级管理员标识，包括用户、建议、评论、评论点赞、建议点赞、题目、刷题汇总、成长等级快照、系统设置和审计字段。

当前边界说明：

- `ai-learn-backend` 负责读写 MySQL。
- Flyway 负责自动执行用户、建议、评论、题目、刷题汇总、RAG 任务和成长体系相关表 migration。
- 密码只保存 BCrypt 哈希，禁止保存明文密码。
- `sprint2619` 后，建议区不再保存处理状态和标题；建议与评论均通过点赞明细表记录用户点赞状态，评论支持一级父子评论。
- `sprint2620` 后，成长徽章只保留 AI 智能刷题联动的 11 个勋章，并通过 `user_practice_sessions.discussion_follow_up_count` 记录当前题评分后的连续追问次数。`sprint2621` 后，个人中心不再维护成长明细流水，徽章、学习天数和经验均基于汇总表计算。
- 系统题库通过 migration 初始化 `AI面试题Top300.csv` 中的真实题目数据，供热门面经和 AI 智能刷题使用。
- `users.super_admin` 用于标识超级管理员，默认注册用户为普通用户，只允许后台开发者通过数据库维护。`sprint202613` 后，`questions.code` 是题目稳定业务编码，`questions.question_type` 是分类字符串来源，所有下拉分类从题目表 `DISTINCT question_type` 获取；系统不再创建 `knowledge_points` 与 `question_knowledge_points`。
- Redis、Qdrant 等中间件不参与建议评论区最小闭环。
- `system_settings` 保存系统最大用户数等轻量配置，当前使用 `MAX_USERS` 控制开放注册容量。

## 2. 推荐版本

推荐使用：

```text
MySQL 8.4 LTS
```

选择理由：

- LTS 版本适合长期维护。
- 本地和服务器保持同一主版本，减少 SQL 行为差异。
- Spring Boot 使用 MySQL Connector/J 连接 MySQL 8.x。
- 后端显式使用 Flyway `12.6.1`，避免 MySQL 8.4 LTS 启动时出现旧版 Flyway 兼容性告警。

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
SHOW INDEX FROM users;
SHOW INDEX FROM suggestions;
SHOW INDEX FROM comments;
SHOW INDEX FROM questions;
SELECT id, username, super_admin FROM users WHERE username = '本地用户名占位符';
```

后端验证：

1. 设置本地环境变量 `DATABASE_PASSWORD` 和 `JWT_SECRET`。
2. 启动 `ai-learn-backend`。
3. 确认 Flyway 已执行 `V1` 到 `V18` migration，包含用户、互动、题库、刷题、RAG、成长徽章、超级管理员标识、当前题答案记忆字段、修仙境界默认值刷新、建议评论区评论流重构、刷题勋章强联动和系统设置表。
4. 调用 `/api/v1/auth/register` 注册用户。
5. 查询 `users.password_hash`，确认保存的是 BCrypt 哈希而不是明文密码。
6. 调用 `/api/v1/auth/login` 获取 token。
7. 使用 `Authorization: Bearer <accessToken占位符>` 调用 `/api/v1/users/me`。
8. 以游客身份调用 `GET /api/v1/suggestions?pageNo=1&pageSize=10&sort=hot` 和 `GET /api/v1/comments?pageNo=1&pageSize=10&sort=hot`，确认可公开分页查询。
9. 使用 `Authorization: Bearer <accessToken占位符>` 调用 `POST /api/v1/suggestions` 和 `POST /api/v1/comments`，确认登录用户可发布纯文字建议和评论。
10. 查询 `suggestions.like_count` 和 `comments.like_count` 默认为 `0`，并确认 `suggestions` 不再包含 `title`、`status` 字段。
11. 使用 `Authorization: Bearer <accessToken占位符>` 调用 `GET /api/v1/questions?pageNo=1&pageSize=10`，确认可分页查询系统题库。
12. 调用 `GET /api/v1/questions/types`，确认可从题目表查询分类下拉数据。
13. 调用 `GET /api/v1/questions/<题目ID占位符>`，确认可查看题目、参考答案、分类、重要性评分和真实面试出现次数。
14. 注册一个本地普通用户，确认 `users.super_admin` 默认等于 `0`。
15. 使用 `Authorization: Bearer <accessToken占位符>` 调用 `POST /api/v1/suggestions/<建议ID占位符>/like` 和 `POST /api/v1/comments/<评论ID占位符>/like`，确认点赞明细表写入并可再次调用取消点赞。
16. 如需验收超级管理员入口，可在本地测试库执行 `UPDATE users SET super_admin = 1 WHERE username = '本地用户名占位符';`，重新登录后确认前端展示“管理者中心”。
17. 调用 AI 智能刷题评分接口后，查询 `badges`、`user_badges` 和 `user_practice_sessions.discussion_follow_up_count`，确认 sprint2620 勋章发放与追问计数正常。
18. 使用超级管理员 token 调用 `GET /api/v1/admin/users/limit` 和 `PUT /api/v1/admin/users/limit`，确认 `system_settings.MAX_USERS` 可维护。
19. 将最大用户数设置为当前用户数后，再调用 `/api/v1/auth/register`，确认返回“当前系统用户数量已达上限，等待管理员升级服务器并扩容”。

## 8. 后续部署到服务器注意事项

- 禁止使用 root 账号作为业务账号。
- 生产密码和 `JWT_SECRET` 必须使用高强度随机值，并通过环境变量、服务器私有配置或密钥管理系统注入。
- MySQL 端口不直接暴露公网，只允许应用服务器或可信内网访问。
- 数据目录必须持久化，并制定备份策略，至少包含定期全量备份和恢复演练。
- 上线前确认字符集、时区、排序规则与本地环境一致。
- 表结构变更必须通过 Flyway migration 发布，不允许只在生产库手工改表。
- 生产环境调整超级管理员必须走审批流程，执行 SQL 时只能更新明确账号，禁止批量无条件更新 `users.super_admin`。
- 日志和监控中不得输出完整连接串、用户名密码、JWT token 或敏感业务数据。

## 9. sprint202612/sprint202613 系统题库与刷题汇总说明

本迭代已将 `questions` 表简化为系统题库核心字段，并新增 `V11__system_question_bank_and_practice_summary.sql` 支持轻量刷题汇总。

新增或调整字段：

| 表 | 字段 | 用途 |
| --- | --- | --- |
| `questions` | `code` | 题目稳定业务编码，用户刷题汇总按该字段关联题目 |
| `questions` | `question` | 新版题目正文 |
| `questions` | `question_type` | 题目分类字符串；前端下拉框从题目表 distinct 统计获得 |
| `questions` | `standard_answer` | 参考答案 |
| `questions` | `importance_score` | 抽题权重使用的重要性评分，0-100，支持 1 位小数 |
| `questions` | `occurrence_count` | 真实面试出现次数，用于辅助抽题权重 |
| `user_question_stats` | 全表 | 保存用户、题目编码维度的答题次数、最高分和最近得分 |
| `user_practice_sessions` | 全表 | 保存用户当前刷题阶段、当前题目编码和当前题最近一次答案，不保存完整聊天明细 |
| `user_practice_sessions` | `last_answer_text` | 当前题最近一次答案原文，用于本题讨论阶段让 AI 记住刚刚的回答 |

本地验证 SQL：

```sql
DESC questions;
DESC user_question_stats;
DESC user_practice_sessions;
SELECT COUNT(1) FROM questions WHERE deleted = 0;
SELECT DISTINCT question_type FROM questions WHERE deleted = 0 ORDER BY question_type;
SELECT code, question_type, importance_score, occurrence_count FROM questions WHERE deleted = 0 LIMIT 5;
SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'FOREIGN KEY';
SELECT user_id, question_code, answer_count, best_score, last_score FROM user_question_stats WHERE user_id = 用户ID占位符;
SELECT user_id, question_code, phase, last_score, LEFT(last_answer_text, 80) AS last_answer_preview FROM user_practice_sessions WHERE user_id = 用户ID占位符;
```

注意事项：

- `questions.code` 必须保持稳定；CSV 初始化数据在编码为空时使用 `AI-TOP-0001` 到 `AI-TOP-0300` 自动编码，管理员删除后再次导入同编码题目时，系统会按编码恢复或更新题目。
- `questions` 不再保留 `title`、`content`、`difficulty`、`tags`、`analysis`、`owner_user_id`、`source_type` 等历史字段，避免个人题库和系统题库混用。
- 不再通过用户自定义题库承载刷题题目，普通用户只能从系统题库刷题。
- 不新增完整聊天记录表，也不新增完整答题记录表；`last_answer_text` 只保存当前题最近一次答案，用于讨论上下文，不用于长期答题明细沉淀。
- 生产排查时禁止把 `last_answer_text` 全量打印到日志，只允许按需查看脱敏片段或在授权范围内临时查询。
- 系统表不再定义数据库外键，删除和清理数据由业务逻辑与索引约束保证，避免本地调试时被外键阻塞。
- `V12__remove_foreign_keys_and_knowledge_tables.sql` 会兼容已初始化数据库：移除历史外键、删除知识点表和关系表，并用 Top300 CSV 题库重置系统题目。
- 生产部署前必须先备份 MySQL，再发布 Flyway migration；全新环境可直接执行最新 migration 初始化表结构。

## 10. sprint2617 成长等级与修仙境界段位说明

本迭代新增 `V14__refresh_growth_realm_defaults.sql`，用于配合成长体系从旧等级名称和通用段位迁移到 `LV + 修仙境界` 展示。

调整内容：

| 表 | 字段 | 调整 | 用途 |
| --- | --- | --- | --- |
| `users` | `level_code` | 默认值保持 `LV1` | 保存当前等级编码，等级展示由后端按总经验动态计算 |
| `users` | `rank_code` | 扩大为 `VARCHAR(32)`，默认值改为 `QI_REFINING` | 保存修仙境界编码，兼容 `FOUNDATION_BUILDING` 等较长编码 |

成长计算说明：

- 总经验仍按 `user_question_stats.best_score` 汇总得到，即所有题目历史最高分之和。
- 每 300 总经验升 1 级，展示格式为 `LV3 700/900`。
- 段位由等级范围映射得到，例如 `LV1~LV10` 为炼气期，`LV11~LV20` 为筑基期。
- 历史 `BRONZE`、`SILVER`、`GOLD`、`PLATINUM`、`DIAMOND`、`KING` 会先回收为 `QI_REFINING`，用户登录、查询个人信息或完成答题后会按真实总经验刷新为正确境界编码。

本地验证 SQL：

```sql
DESC users;
SELECT version, description, success FROM flyway_schema_history WHERE version = '14';
SELECT id, username, experience, level_code, rank_code FROM users WHERE username = '本地用户名占位符';
SELECT COALESCE(SUM(best_score), 0) AS total_experience FROM user_question_stats WHERE user_id = 用户ID占位符;
```

部署注意事项：

- 发布前必须备份 MySQL，确认 `rank_code` 字段长度已扩大到 32。
- 不允许在生产库手工写入旧通用段位编码，新段位编码必须使用后端 `GrowthRank` 统一计算。
- 排查成长数据时只能查询必要账号，禁止导出全量用户成长数据到不受控环境。


## 11. sprint2619 建议评论区评论流说明

本迭代新增 `V15__redesign_interaction_comments.sql`，用于把建议评论区从旧表单状态模型调整为轻量评论流模型。

新增或调整内容：

| 表 | 字段或索引 | 用途 |
| --- | --- | --- |
| `suggestions` | 删除 `title` | 建议区不再填写标题，只保留正文。 |
| `suggestions` | 删除 `status` | 建议无需待处理、已处理状态。 |
| `suggestions` | 新增 `like_count` | 支持建议按点赞数最热排序。 |
| `comments` | 新增父级和点赞排序索引 | 支持父评论分页、一级子评论查询和最热排序。 |
| `comment_likes` | 全表 | 保存用户对评论的点赞状态，避免重复点赞。 |
| `suggestion_likes` | 全表 | 保存用户对建议的点赞状态，避免重复点赞。 |

本地验证 SQL：

```sql
DESC suggestions;
DESC comments;
DESC comment_likes;
DESC suggestion_likes;
SELECT version, description, success FROM flyway_schema_history WHERE version = '15';
SHOW INDEX FROM suggestions;
SHOW INDEX FROM comments;
SELECT id, type, content, like_count FROM suggestions ORDER BY created_at DESC LIMIT 5;
SELECT id, parent_id, content, like_count FROM comments ORDER BY created_at DESC LIMIT 5;
```

本地联调接口：

```text
GET  /api/v1/suggestions?pageNo=1&pageSize=10&sort=hot
GET  /api/v1/suggestions?pageNo=1&pageSize=10&sort=latest
POST /api/v1/suggestions
POST /api/v1/suggestions/<建议ID占位符>/like
GET  /api/v1/comments?pageNo=1&pageSize=10&sort=hot
GET  /api/v1/comments?pageNo=1&pageSize=10&sort=latest
POST /api/v1/comments
POST /api/v1/comments/<评论ID占位符>/like
```

部署注意事项：

- 发布前必须备份 MySQL，确认旧建议数据中的 `type` 已被迁移到功能建议、体验优化、问题反馈、内容建议四类之一。
- 生产库不允许手工写入 `comment_likes`、`suggestion_likes`，点赞状态必须通过后端接口生成。
- 建议和评论正文只允许纯文字；排查数据时不要把用户原文批量导出到不受控环境。



## 12. sprint2620 刷题勋章强联动说明

本迭代新增 `V16__refresh_practice_badges.sql`，用于刷新 AI 智能刷题勋章定义，并为“问到底”勋章增加当前题追问计数字段。

新增或调整内容：

| 表 | 字段或数据 | 用途 |
| --- | --- | --- |
| `badges` | 刷新为 11 个固定勋章 | 只保留初次启程、十题小成、百题修炼、大成圆满、三日不辍、月度坚持者、百日成神、深夜修行者、清晨启动者、周末不摆烂、问到底。 |
| `user_badges` | 清理旧规则记录 | 删除不在 sprint2620 范围内的历史徽章记录，避免个人中心展示旧勋章。 |
| `user_practice_sessions` | `discussion_follow_up_count` | 记录当前题评分后连续有效追问次数，达到 3 次发放“问到底”。 |

本地验证 SQL：

```sql
DESC badges;
DESC user_badges;
DESC user_practice_sessions;
SELECT version, description, success FROM flyway_schema_history WHERE version = '16';
SELECT rule_code, name, description FROM badges ORDER BY id;
SELECT COUNT(1) AS badge_count FROM badges;
SELECT user_id, question_code, phase, discussion_follow_up_count FROM user_practice_sessions WHERE user_id = 用户ID占位符;
SELECT ub.user_id, b.rule_code, b.name, ub.acquired_at FROM user_badges ub JOIN badges b ON b.id = ub.badge_id WHERE ub.user_id = 用户ID占位符 ORDER BY ub.acquired_at DESC;
```

本地联调场景：

```text
GET  /api/v1/growth/me
POST /api/v1/practice/messages
POST /api/v1/practice/messages/stream
```

联调关注点：

- 完成刷题评分后，`user_question_stats.answer_count` 增加，后端按累计完成次数、学习天数、时段和周末发放勋章，不再写入成长明细流水。
- 单题评分后连续有效追问 3 次，`discussion_follow_up_count` 达到 3，并尝试发放“问到底”。
- 个人中心徽章墙只展示 sprint2620 定义内的勋章；隐藏/稀有类未获得时不展示。

部署注意事项：

- 发布前必须备份 MySQL，确认旧徽章清理符合产品预期。
- 生产库不允许手工补发勋章，特殊处理需走审批并通过明确用户和明确规则编码执行。
- 排查勋章问题时只查询必要用户，禁止导出全量用户刷题数据到不受控环境。
- 应用服务器时区建议与 `DATABASE_URL` 中的 `serverTimezone=Asia/Shanghai` 保持一致，避免深夜、清晨和周末勋章判断出现偏差。

## 13. sprint2621 用户管理与容量限制说明

本迭代新增 `V17__system_settings_and_user_limit.sql`，用于保存系统级轻量配置；新增 `V18__drop_growth_events.sql`，用于删除已下线的成长明细流水表。

新增或调整内容：

| 表 | 字段或数据 | 用途 |
| --- | --- | --- |
| `system_settings` | 全表 | 保存系统设置键值对，示例值统一使用占位或默认值，不保存密码、Token 等敏感信息。 |
| `system_settings` | `MAX_USERS` | 控制开放注册最大用户数，达到上限后注册接口返回固定扩容提示。 |
| `users` | 管理端 CRUD | 超级管理员可在页面新增、编辑、逻辑删除和查询用户。 |
| `questions` | TRUNCATE 清空 | 系统题库管理的一键清空功能会真实清空题库主表并重置自增ID。 |

本地验证 SQL：

```sql
DESC system_settings;
SELECT setting_key, setting_value FROM system_settings WHERE setting_key = 'MAX_USERS';
SELECT COUNT(1) AS current_users FROM users WHERE deleted = 0;
SELECT COUNT(1) AS question_count FROM questions;
```

本地联调场景：

```text
GET    /api/v1/admin/users?pageNo=1&pageSize=10
POST   /api/v1/admin/users
PUT    /api/v1/admin/users/<用户ID占位符>
DELETE /api/v1/admin/users/<用户ID占位符>
GET    /api/v1/admin/users/limit
PUT    /api/v1/admin/users/limit
DELETE /api/v1/admin/system-questions/clear
```

部署注意事项：

- 发布前必须备份 MySQL，特别是一键清空题库会执行 `TRUNCATE TABLE questions`，生产环境只能在明确审批后使用。
- `MAX_USERS` 仅保存数字，不需要也不得保存任何服务器规格、价格、密钥或生产连接信息。
- 管理员删除用户采用逻辑删除，避免误删后影响历史互动展示；如需物理清理必须另走数据治理流程。
- 生产环境调整最大用户数时，需要同步评估应用服务器、MySQL 连接池和 AI 服务容量。

