## ADDED Requirements

### Requirement: 前端工程可本地启动
系统 SHALL 提供 `ai-learn-web` 前端工程，并配置 Vue 3、Vite、TypeScript、Vue Router 和 Element Plus，以支持本地开发启动和生产构建。

#### Scenario: 本地启动前端工程
- **WHEN** 开发者按照启动说明在 `ai-learn-web` 中安装依赖并执行本地启动命令
- **THEN** 前端应用 SHALL 在本地开发服务中启动，并能够通过浏览器访问

#### Scenario: 构建前端工程
- **WHEN** 开发者在 `ai-learn-web` 中执行生产构建命令
- **THEN** 前端工程 SHALL 完成构建，且不依赖真实密码、Token、密钥或生产连接地址

### Requirement: 默认路由进入学习路线页面
系统 SHALL 将前端根路径 `/` 重定向到 `/learning-roadmap`，并默认展示“AI 学习路线与资料”页面。

#### Scenario: 访问根路径
- **WHEN** 用户在浏览器访问前端根路径 `/`
- **THEN** 系统 SHALL 自动进入 `/learning-roadmap` 并高亮“AI 学习路线与资料”菜单

### Requirement: 全局布局展示规划菜单
系统 SHALL 提供清新简约的全局布局，包含左侧菜单、顶部用户区和主内容区；左侧菜单 SHALL 展示“AI 学习路线与资料”“建议与评论”“面试题大全”“刷题 Agent”四个入口。

#### Scenario: 查看全局布局
- **WHEN** 用户打开任意前端页面
- **THEN** 系统 SHALL 展示左侧四个规划菜单、顶部用户区和主内容区

#### Scenario: 切换菜单高亮
- **WHEN** 用户点击任一左侧菜单入口
- **THEN** 系统 SHALL 跳转到对应路由，并高亮当前菜单

### Requirement: 顶部用户入口仅作为占位
系统 SHALL 在顶部用户区展示“登录 / 注册”入口占位，且不得实现真实登录、注册、退出、JWT 或权限守卫。

#### Scenario: 点击登录注册占位
- **WHEN** 用户点击“登录 / 注册”入口
- **THEN** 系统 SHALL 使用中文提示“登录能力将在 sprint202602 开放”或等价后续迭代开放说明

### Requirement: 非本期菜单展示占位页
系统 SHALL 为“建议与评论”“面试题大全”“刷题 Agent”提供占位页面，页面包含明确标题和后续迭代说明，不得出现接口报错或空白页。

#### Scenario: 打开建议与评论占位页
- **WHEN** 用户访问 `/suggestions-comments`
- **THEN** 系统 SHALL 展示“建议与评论”标题和后续迭代开放说明

#### Scenario: 打开面试题大全占位页
- **WHEN** 用户访问 `/interview-questions`
- **THEN** 系统 SHALL 展示“面试题大全”标题和后续迭代开放说明

#### Scenario: 打开刷题 Agent 占位页
- **WHEN** 用户访问 `/practice-agent`
- **THEN** 系统 SHALL 展示“刷题 Agent”标题和后续迭代开放说明

### Requirement: 前端 API 地址使用占位配置
系统 SHALL 提供 `.env.example`，其中包含本地开发 API 基础地址占位配置 `VITE_API_BASE_URL=http://localhost:8080/api/v1`。

#### Scenario: 查看前端环境示例
- **WHEN** 开发者打开 `ai-learn-web/.env.example`
- **THEN** 文件 SHALL 包含 `VITE_API_BASE_URL` 本地占位值，且 SHALL NOT 包含真实生产地址或敏感信息
