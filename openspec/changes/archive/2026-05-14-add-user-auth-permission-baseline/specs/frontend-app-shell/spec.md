## MODIFIED Requirements

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

## ADDED Requirements

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
