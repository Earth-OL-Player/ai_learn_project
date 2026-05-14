# learning-roadmap Specification

## Requirements

### Requirement: 学习路线页面静态渲染前端 Markdown
系统 SHALL 将前端项目内的 `AI应用开发学习路线和资料集.md` 原文渲染为“路线和资料”页面，并 SHALL NOT 调用后端接口获取学习路线页面内容。

#### Scenario: 打开学习路线页面
- **WHEN** 用户访问 `/learning-roadmap`
- **THEN** 系统 SHALL 渲染前端项目内 Markdown 文件中的标题、正文、表格、引用、列表、链接和图片

#### Scenario: 页面内容来源检查
- **WHEN** 用户打开学习路线页面
- **THEN** 页面 SHALL NOT 请求 `GET /api/v1/learning/roadmap` 获取内容

### Requirement: 学习路线 Markdown 支持后续直接维护
系统 SHALL 将 Markdown 文件和图片资源目录放在前端项目中，开发者修改该 Markdown 文件后页面内容 SHALL 可同步更新。

#### Scenario: 修改前端 Markdown 文件
- **WHEN** 开发者修改 `ai-learn-web/src/content/learning-roadmap/AI应用开发学习路线和资料集.md`
- **THEN** 开发环境页面 SHALL 在 Vite 热更新或重新构建后展示更新后的 Markdown 内容

#### Scenario: 展示 Markdown 相对路径图片
- **WHEN** Markdown 使用同级 `.assets` 目录中的相对路径图片
- **THEN** 页面 SHALL 正确展示该图片

### Requirement: 学习路线 Markdown 原文不得被改写
系统 SHALL 保持复制到前端项目中的 Markdown 内容与来源文档一致，页面渲染逻辑仅处理展示样式、标题锚点、链接属性、图片资源地址和图注展示。

#### Scenario: 校验 Markdown 内容一致
- **WHEN** 比较来源 Markdown 与前端项目内 Markdown 文件
- **THEN** 两个文件内容 SHALL 保持一致，除非开发者明确直接维护前端项目内 Markdown 文件

### Requirement: 学习路线页面展示目录
系统 SHALL 根据 Markdown 二级到四级标题在页面内容区左侧生成“目录”，并 SHALL 允许用户点击目录跳转到对应章节，且 SHALL 高亮当前阅读章节。

#### Scenario: 查看目录
- **WHEN** 用户打开学习路线页面
- **THEN** 页面 SHALL 在内容区左侧展示由 Markdown 标题生成的“目录”

#### Scenario: 点击目录跳转章节
- **WHEN** 用户点击目录中的章节
- **THEN** 页面 SHALL 跳转到对应 Markdown 标题位置

#### Scenario: 当前章节高亮
- **WHEN** 用户滚动到某个 Markdown 章节
- **THEN** 目录中对应章节 SHALL 显示特殊高亮标识

### Requirement: 学习路线目录支持收起和展开
系统 SHALL 支持用户收起和展开学习路线页面左侧目录，收起后正文区域 SHALL 获得更大展示空间。

#### Scenario: 收起目录
- **WHEN** 用户点击目录收起按钮
- **THEN** 左侧目录 SHALL 收缩为窄栏，并保留展开入口

#### Scenario: 展开目录
- **WHEN** 用户点击目录展开按钮
- **THEN** 左侧目录 SHALL 恢复完整目录列表

### Requirement: 学习路线页面不展示内部维护提示
系统 SHALL NOT 在面向用户的学习路线页面展示 Markdown 维护方式、文件路径或内部开发提示。

#### Scenario: 查看学习路线页面顶部
- **WHEN** 用户打开学习路线页面
- **THEN** 页面 SHALL NOT 展示“本地 Markdown 静态页面”或“直接修改前端 Markdown 文件即可同步页面内容”等内部提示

### Requirement: Markdown 图片展示图注
系统 SHALL 根据 Markdown 图片替代文本在图片下方展示图注，格式为“图序号-图片名称”。

#### Scenario: 展示路线图图注
- **WHEN** Markdown 中存在 `![AI应用开发学习路线](...)` 图片
- **THEN** 页面 SHALL 在图片下方展示“图1-AI应用开发学习路线”

#### Scenario: 多张图片自动编号
- **WHEN** Markdown 中存在多张图片
- **THEN** 页面 SHALL 按图片出现顺序递增展示图注序号
