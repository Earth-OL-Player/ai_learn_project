# frontend-app-shell Specification

## Purpose

定义前端应用外壳、默认路由、全局布局、菜单切换、顶部用户区、登录注册交互、登录态恢复、权限引导和 API 地址占位配置，确保用户具备一致清新简约的前端访问体验。

## Requirements

### Requirement: 前端工程可本地启动
系统 SHALL 提供 `ai-learn-web` 前端工程，并配置 Vue 3、Vite、TypeScript、Vue Router 和 Element Plus，以支持本地开发启动和生产构建。

#### Scenario: 本地启动前端工程
- **WHEN** 开发者按照启动说明在 `ai-learn-web` 中安装依赖并执行本地启动命令
- **THEN** 前端应用 SHALL 在本地开发服务中启动，并能够通过浏览器访问

#### Scenario: 构建前端工程
- **WHEN** 开发者在 `ai-learn-web` 中执行生产构建命令
- **THEN** 前端工程 SHALL 完成构建，且不依赖真实密码、Token、密钥或生产连接地址

### Requirement: 默认路由进入学习路线页面
系统 SHALL 将前端根路径 `/` 重定向到 `/learning-roadmap`，并默认展示“路线和资料”页面。

#### Scenario: 访问根路径
- **WHEN** 用户在浏览器访问前端根路径 `/`
- **THEN** 系统 SHALL 自动进入 `/learning-roadmap` 并高亮“路线和资料”菜单

### Requirement: 全局布局展示规划菜单
系统 SHALL 提供清新简约的全局布局，包含左侧菜单、顶部用户区和主内容区；左侧菜单 SHALL 展示“路线和资料”“建议评论区”“热门面经”“AI智能刷题”四个入口。

#### Scenario: 查看全局布局
- **WHEN** 用户打开任意前端页面
- **THEN** 系统 SHALL 展示左侧四个规划菜单、顶部用户区和主内容区

#### Scenario: 切换菜单高亮
- **WHEN** 用户点击任一左侧菜单入口
- **THEN** 系统 SHALL 跳转到对应路由，并高亮当前菜单

### Requirement: 顶部用户入口仅作为占位
系统 SHALL 将顶部用户区从“登录 / 注册”占位升级为真实用户入口：游客 SHALL 看到登录和注册入口，登录用户 SHALL 看到用户摘要、个人中心入口和退出登录入口。

#### Scenario: 游客查看顶部用户区
- **WHEN** 游客打开任意前端页面
- **THEN** 系统 SHALL 在顶部用户区展示可打开真实登录和注册弹窗的入口

#### Scenario: 登录用户查看顶部用户区
- **WHEN** 登录用户打开任意前端页面
- **THEN** 系统 SHALL 在顶部用户区展示当前用户昵称或用户名，并提供个人中心入口和退出登录入口

#### Scenario: 登录用户退出登录
- **WHEN** 登录用户点击退出登录入口并退出成功
- **THEN** 系统 SHALL 清理本地 token 和用户信息，并将顶部用户区恢复为游客登录注册入口

### Requirement: 顶部不展示当前页面标题区
系统 SHALL NOT 在顶部展示“当前页面 XXXXX”标题区，避免占用页面纵向空间。

#### Scenario: 打开任意页面
- **WHEN** 用户打开任意前端页面
- **THEN** 顶部 SHALL NOT 展示“当前页面”文案或当前页面标题区

### Requirement: 非本期菜单展示占位页
系统 SHALL 为“建议评论区”“热门面经”“AI智能刷题”提供占位页面，页面包含明确标题和后续迭代说明，不得出现接口报错或空白页。

#### Scenario: 打开建议评论区占位页
- **WHEN** 用户访问 `/suggestions-comments`
- **THEN** 系统 SHALL 展示“建议评论区”标题和后续迭代开放说明

#### Scenario: 打开热门面经占位页
- **WHEN** 用户访问 `/interview-questions`
- **THEN** 系统 SHALL 展示“热门面经”标题和后续迭代开放说明

#### Scenario: 打开AI智能刷题 占位页
- **WHEN** 用户访问 `/practice-agent`
- **THEN** 系统 SHALL 展示“AI智能刷题”标题和后续迭代开放说明

### Requirement: 前端 API 地址使用占位配置
系统 SHALL 提供 `.env.example`，其中包含本地开发 API 基础地址占位配置 `VITE_API_BASE_URL=http://localhost:8080/api/v1`。

#### Scenario: 查看前端环境示例
- **WHEN** 开发者打开 `ai-learn-web/.env.example`
- **THEN** 文件 SHALL 包含 `VITE_API_BASE_URL` 本地占位值，且 SHALL NOT 包含真实生产地址或敏感信息


### Requirement: 前端登录态支持刷新恢复
系统 SHALL 使用 Pinia 管理登录态，并将 access token 保存到本地存储 key `ai_learn_access_token`，页面刷新后 SHALL 能通过 `/api/v1/users/me` 恢复当前用户信息。

#### Scenario: 页面刷新后恢复登录态
- **WHEN** 登录用户刷新浏览器页面且本地存在有效 token
- **THEN** 系统 SHALL 调用 `/api/v1/users/me` 恢复当前用户信息，并保持顶部用户区为登录状态

#### Scenario: 页面刷新后 token 已失效
- **WHEN** 用户刷新浏览器页面且本地 token 无效或已过期
- **THEN** 系统 SHALL 清理本地 token 和用户信息，并在访问受保护页面时展示登录引导

### Requirement: 受保护入口展示登录引导
系统 SHALL 对热门面经、AI智能刷题和个人中心等受保护入口进行权限判断，游客访问时 SHALL 统一提示“登录后即可使用该功能”。

#### Scenario: 游客访问热门面经
- **WHEN** 游客访问 `/interview-questions`
- **THEN** 系统 SHALL 展示“登录后即可使用该功能”或等价中文登录引导，不得展示接口报错或空白页

#### Scenario: 游客访问AI智能刷题
- **WHEN** 游客访问 `/practice-agent`
- **THEN** 系统 SHALL 展示“登录后即可使用该功能”或等价中文登录引导，不得展示接口报错或空白页

#### Scenario: 游客访问个人中心
- **WHEN** 游客访问个人中心路由
- **THEN** 系统 SHALL 展示“登录后即可使用该功能”或等价中文登录引导，不得展示个人信息内容

#### Scenario: 游客访问学习路线
- **WHEN** 游客访问 `/learning-roadmap`
- **THEN** 系统 SHALL 允许访问学习路线页面，不要求登录鉴权

#### Scenario: 游客访问建议评论区占位页
- **WHEN** 游客访问 `/suggestions-comments`
- **THEN** 系统 SHALL 允许访问建议评论区占位页，不要求登录鉴权

### Requirement: 前端提供登录注册弹窗
系统 SHALL 提供清新简约的登录弹窗和注册弹窗，支持调用后端注册、登录接口并处理成功、校验失败和认证失败反馈。

#### Scenario: 打开登录弹窗
- **WHEN** 游客点击登录入口
- **THEN** 系统 SHALL 展示包含用户名和密码输入项的登录弹窗

#### Scenario: 打开注册弹窗
- **WHEN** 游客点击注册入口
- **THEN** 系统 SHALL 展示包含用户名、密码、必填昵称和必填邮箱输入项的注册弹窗

#### Scenario: 登录成功后更新界面
- **WHEN** 游客在登录弹窗提交正确用户名和密码并登录成功
- **THEN** 系统 SHALL 保存 token 和用户信息、关闭弹窗，并将顶部用户区更新为登录状态

#### Scenario: 注册成功后更新界面
- **WHEN** 游客在注册弹窗提交合法且未占用的注册信息并注册成功
- **THEN** 系统 SHALL 保存 token 和用户信息、关闭弹窗，并将顶部用户区更新为登录状态

### Requirement: 前端支持活跃自动续期
系统 SHALL 在接口响应中读取 `X-Refresh-Token` 响应头；当该响应头存在时，系统 SHALL 使用新 token 覆盖本地存储 key `ai_learn_access_token`。

#### Scenario: 接口响应包含续期 token
- **WHEN** 登录用户调用后端接口且响应头包含 `X-Refresh-Token`
- **THEN** 前端 SHALL 将该响应头中的新 token 保存到 `ai_learn_access_token`

#### Scenario: 接口响应不包含续期 token
- **WHEN** 客户端调用后端接口且响应头不包含 `X-Refresh-Token`
- **THEN** 前端 SHALL 保持当前登录态处理逻辑不变
