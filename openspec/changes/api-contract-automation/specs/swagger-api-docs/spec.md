## ADDED Requirements

### Requirement: Swagger UI 可访问
系统 SHALL 在 dev 环境下通过 `/swagger-ui.html` 路径提供 Swagger UI 界面，可通过 Spring Boot 应用的 HTTP 端口访问。

#### Scenario: 开发者访问 Swagger UI
- **WHEN** 开发者在 dev 环境访问 `http://localhost:8080/swagger-ui.html`
- **THEN** 返回 Swagger UI HTML 页面，包含所有 API 接口列表

### Requirement: OpenAPI JSON 文档可获取
系统 SHALL 在 `/v3/api-docs` 路径提供 OpenAPI 3.1 格式的 JSON 文档，包含所有 Controller 的完整接口定义。

#### Scenario: 获取 OpenAPI 文档
- **WHEN** 发送 GET 请求到 `/v3/api-docs`
- **THEN** 返回 openapi: "3.1.0" 的 JSON 文档，包含所有 14 个接口的 paths、components/schemas 定义

### Requirement: 所有 Controller 接口有 OpenAPI 注解
所有 REST Controller 中的每个接口方法 SHALL 标注 @Operation，包含 summary 和 description，Controller 类 SHALL 标注 @Tag。

#### Scenario: 项目管理 Controller 注解完整
- **WHEN** 查看 ProjectController 的 OpenAPI 文档
- **THEN** 6 个接口（创建/更新/查询/列表/删除/设置基础论文）均包含 operationId、summary、description

#### Scenario: 论文检索 Controller 注解完整
- **WHEN** 查看 PaperController 的 OpenAPI 文档
- **THEN** 4 个接口（搜索/列表/详情/任务状态）均包含 operationId、summary、description、参数描述

#### Scenario: 分析 Controller 注解完整
- **WHEN** 查看 AnalysisController 的 OpenAPI 文档
- **THEN** 3 个接口（触发分析/查询结果/任务状态）均包含 operationId、summary、description

#### Scenario: 定时任务 Controller 注解完整
- **WHEN** 查看 SchedulerJobController 的 OpenAPI 文档
- **THEN** 1 个接口（执行任务）包含 operationId、summary、description 和 404 错误响应

### Requirement: 所有 DTO/Request 字段有 Schema 注解
所有请求和响应 DTO 中的每个字段 SHALL 标注 @Schema，包含 description 和 example 值，类级别标注 title 和 description。

#### Scenario: 请求 DTO 字段注解
- **WHEN** 查看 CreateProjectRequest 的 Schema 定义
- **THEN** name、description、topic 字段各有 description 和 example，name 和 topic 标注 required

#### Scenario: 响应 DTO 字段注解
- **WHEN** 查看 ProjectDTO 的 Schema 定义
- **THEN** id、name、description、topic、createTime、updateTime、status 字段各有 description 和 example

#### Scenario: ApiResponse 包装层注解
- **WHEN** 查看 ApiResponse 的 Schema 定义
- **THEN** code、message、data、timestamp 字段各有 description，data 字段使用泛型标注

### Requirement: 接口参数验证在文档中体现
所有使用 @Valid、@Validated、@NotNull、@NotBlank、@Min 等验证注解的参数，SHALL 在 OpenAPI 文档中标注 required 属性和约束。

#### Scenario: 分页参数验证
- **WHEN** 查看 GET /api/v1/papers 接口文档
- **THEN** projectId 标注 required，page 标注 minimum: 1，size 标注 minimum: 1，default 值正确显示

#### Scenario: 请求体验证
- **WHEN** 查看 POST /api/v1/projects 接口文档
- **THEN** 请求体标注 required，CreateProjectRequest 中 name 和 topic 字段标注 required

### Requirement: Swagger UI 仅 dev 环境启用
系统 SHALL 仅在 dev profile 激活时启用 Swagger UI，prod 环境默认关闭。

#### Scenario: dev 环境启用
- **WHEN** 以 dev profile 启动应用
- **THEN** Swagger UI 和 OpenAPI JSON 端点均可访问

#### Scenario: prod 环境关闭
- **WHEN** 以 prod profile 启动应用（或未指定 profile）
- **THEN** Swagger UI 和 OpenAPI JSON 端点返回 404 或禁用响应
