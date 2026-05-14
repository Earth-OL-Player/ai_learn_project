## ADDED Requirements

### Requirement: 学习路线页面静态渲染前端 Markdown
系统 SHALL 将前端项目内的 `AI应用开发学习路线和资料集.md` 原文渲染为“AI 学习路线和资料”页面，并 SHALL NOT 调用后端接口获取学习路线页面内容。

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
系统 SHALL 保持复制到前端项目中的 Markdown 内容与来源文档一致，页面渲染逻辑仅处理展示样式、链接属性和图片资源地址。

#### Scenario: 校验 Markdown 内容一致
- **WHEN** 比较来源 Markdown 与前端项目内 Markdown 文件
- **THEN** 两个文件内容 SHALL 保持一致，除非开发者明确直接维护前端项目内 Markdown 文件


### Requirement: 学习路线页面展示文档目录
系统 SHALL 根据 Markdown 二级到四级标题在页面内容区左侧生成“目录”，并 SHALL 允许用户点击目录跳转到对应章节，且 SHALL 高亮当前阅读章节。

#### Scenario: 查看学习文档目录
- **WHEN** 用户打开学习路线页面
- **THEN** 页面 SHALL 在内容区左侧展示由 Markdown 标题生成的“目录”

#### Scenario: 点击目录跳转章节
- **WHEN** 用户点击文档目录中的章节
- **THEN** 页面 SHALL 跳转到对应 Markdown 标题位置

### Requirement: 学习路线页面不展示内部维护提示
系统 SHALL NOT 在面向用户的学习路线页面展示 Markdown 维护方式、文件路径或内部开发提示。

#### Scenario: 查看学习路线页面顶部
- **WHEN** 用户打开学习路线页面
- **THEN** 页面 SHALL NOT 展示“本地 Markdown 静态页面”或“直接修改前端 Markdown 文件即可同步页面内容”等内部提示


补充说明：学习路线页面左侧目录支持收起和展开，收起后正文区域会获得更大展示空间。


补充要求：系统 SHALL 根据 Markdown 图片替代文本在图片下方展示图注，格式为“图序号-图片名称”。
