## ADDED Requirements

### Requirement: OpenAPI JSON 导出到前端项目
构建流程 SHALL 提供从运行中的后端服务获取 OpenAPI 3.1 JSON 文档并保存到前端项目的脚本或 npm script。

#### Scenario: 执行生成脚本
- **WHEN** 开发者运行 `npm run generate:api` 且后端服务正在运行
- **THEN** OpenAPI JSON 文档被下载到前端项目的 `openapi/` 目录

### Requirement: TypeScript 类型自动生成
前端代码生成工具 SHALL 基于 OpenAPI JSON 文档生成所有 DTO 和响应类型的 TypeScript 接口定义。

#### Scenario: 生成项目相关类型
- **WHEN** 执行代码生成
- **THEN** 生成 ProjectDTO、CreateProjectRequest、UpdateProjectRequest、ApiResponse 的 TypeScript 类型

#### Scenario: 生成论文相关类型
- **WHEN** 执行代码生成
- **THEN** 生成 PaperDTO、TaskStatusDTO、分页类型（Page<PaperDTO>）的 TypeScript 类型

#### Scenario: 生成分析相关类型
- **WHEN** 执行代码生成
- **THEN** 生成 AnalysisResult、InnovationAnalysisDTO 等分析相关类型的 TypeScript 类型

### Requirement: API SDK 函数自动生成
前端代码生成工具 SHALL 为每个 REST 接口生成对应的 TypeScript 函数，函数签名包含正确的参数类型和返回类型。

#### Scenario: 生成项目管理 API 函数
- **WHEN** 执行代码生成
- **THEN** 生成 createProject、updateProject、getProject、listProjects、deleteProject、setBasePaper 函数

#### Scenario: 生成论文检索 API 函数
- **WHEN** 执行代码生成
- **THEN** 生成 searchPapers、getPapers、getPaper、getPaperTaskStatus 函数，query 参数正确传递

#### Scenario: 生成分析 API 函数
- **WHEN** 执行代码生成
- **THEN** 生成 triggerAnalysis、getAnalysisResult、getAnalysisTaskStatus 函数

### Requirement: 生成的 SDK 复用现有 axios interceptor
生成的 axios client SHALL 复用前端项目中现有的 response interceptor（自动解包 ApiResponse），生成的 SDK 函数返回类型直接使用 data 字段类型 T。

#### Scenario: SDK 函数返回解包后的数据
- **WHEN** 调用生成的 `getProject({ path: { id: 1 } })` 函数
- **THEN** 返回类型为 ProjectDTO（而非 ApiResponse<ProjectDTO>），interceptor 自动解包

#### Scenario: SDK 函数错误处理
- **WHEN** 后端返回 code !== 200 的响应
- **THEN** interceptor 抛出 ElMessage 错误提示，SDK 函数返回 rejected Promise

### Requirement: 生成代码纳入版本控制
自动生成的 TypeScript 代码文件 SHALL 提交到 git 仓库，确保 CI 可验证契约一致性。

#### Scenario: 生成文件可提交
- **WHEN** 执行代码生成后运行 `git status`
- **THEN** 生成的 .ts 文件显示为 untracked 或 modified，无 .gitignore 规则阻止提交

### Requirement: 代码生成可重复执行
代码生成脚本 SHALL 支持重复执行，每次执行覆盖之前的生成文件，不产生残留。

#### Scenario: 重新生成覆盖旧文件
- **WHEN** 后端接口变更，开发者再次运行 `npm run generate:api`
- **THEN** 旧的生成文件被覆盖，无残留文件，新的类型和函数正确反映最新接口
