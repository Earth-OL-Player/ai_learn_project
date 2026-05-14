## ADDED Requirements

### Requirement: 学习路线接口返回结构化内容
系统 SHALL 提供游客可访问的 `GET /api/v1/learning/roadmap` 接口，并返回统一响应结构中的学习路线数据。

#### Scenario: 查询学习路线成功
- **WHEN** 游客请求 `GET /api/v1/learning/roadmap`
- **THEN** 系统 SHALL 返回 `code` 为 `SUCCESS` 的统一响应，并在 `data` 中包含标题、描述和非空的 `sections`

#### Scenario: 学习路线内容不依赖文档目录
- **WHEN** 后端应用在不包含仓库 `doc/` 目录的部署环境中启动
- **THEN** 学习路线接口 SHALL 仍可返回随应用打包或代码内维护的结构化内容

### Requirement: 学习路线内容覆盖本期阶段
系统 SHALL 在学习路线数据中覆盖平台说明、路线总览、基础阶段、进阶阶段、工程阶段、实战阶段、资料区和学习建议。

#### Scenario: 查看学习路线分区
- **WHEN** 用户打开“AI 学习路线与资料”页面
- **THEN** 页面 SHALL 展示基础阶段、进阶阶段、工程阶段、实战阶段、资料区和学习建议等分区内容

#### Scenario: 查看基础阶段内容
- **WHEN** 用户查看基础阶段
- **THEN** 系统 SHALL 展示 Python、数学基础、机器学习基础相关学习项

#### Scenario: 查看进阶阶段内容
- **WHEN** 用户查看进阶阶段
- **THEN** 系统 SHALL 展示深度学习、NLP、CV、大模型基础相关学习项

#### Scenario: 查看工程阶段内容
- **WHEN** 用户查看工程阶段
- **THEN** 系统 SHALL 展示 LangChain、LangGraph、RAG、向量数据库、模型部署相关学习项

#### Scenario: 查看实战阶段内容
- **WHEN** 用户查看实战阶段
- **THEN** 系统 SHALL 展示 AI Agent、知识库问答、智能刷题、企业应用相关学习项

### Requirement: 学习路线页面消费后端数据
系统 SHALL 通过前端 HTTP 封装请求学习路线接口，并在 `code = SUCCESS` 时渲染响应中的 `data`。

#### Scenario: 成功加载学习路线页面
- **WHEN** 学习路线接口返回 `SUCCESS` 和有效学习路线数据
- **THEN** 前端页面 SHALL 展示接口返回的标题、描述和分区内容

#### Scenario: 学习路线接口业务失败
- **WHEN** 学习路线接口返回非 `SUCCESS` 业务响应
- **THEN** 前端 SHALL 展示响应中的中文 `message`，并避免出现空白页

#### Scenario: 学习路线接口网络异常
- **WHEN** 前端请求学习路线接口发生网络异常
- **THEN** 前端 SHALL 展示“网络异常，请稍后重试”或等价中文提示
