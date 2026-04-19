## 1. 后端 springdoc 集成

- [x] 1.1 在 paper-assistant-web/pom.xml 中添加 springdoc-openapi-starter-webmvc-ui 依赖
- [x] 1.2 在 application-dev.yml 中添加 springdoc 配置（启用 Swagger UI、设置包扫描路径、文档标题/描述）
- [x] 1.3 在 application.yml 或 prod 配置中确保 springdoc 默认关闭

## 2. Controller OpenAPI 注解

- [x] 2.1 ProjectController 添加 @Tag 和所有 6 个接口的 @Operation 注解
- [x] 2.2 PaperController 添加 @Tag 和所有 4 个接口的 @Operation 注解
- [x] 2.3 AnalysisController 添加 @Tag 和所有 3 个接口的 @Operation 注解
- [x] 2.4 SchedulerJobController 添加 @Tag 和 1 个接口的 @Operation 注解

## 3. DTO/Request @Schema 注解

- [x] 3.1 ApiResponse 类添加 @Schema 注解（code、message、data、timestamp 字段）
- [x] 3.2 CreateProjectRequest 类添加 @Schema 注解（name、description、topic 字段）
- [x] 3.3 UpdateProjectRequest 类添加 @Schema 注解
- [x] 3.4 ProjectDTO 类添加 @Schema 注解（所有字段）
- [x] 3.5 PaperDTO 类添加 @Schema 注解（所有字段）
- [x] 3.6 TaskStatusDTO 类添加 @Schema 注解
- [x] 3.7 PaperAnalysisDTO、CrossAnalysisDTO、InnovationAnalysisDTO 添加 @Schema 注解
- [x] 3.8 AnalysisResult 实体（如用作响应）添加 @Schema 注解

## 4. 验证 Swagger UI 和 OpenAPI 文档

- [x] 4.1 启动后端服务，确认 Swagger UI 在 /swagger-ui.html 可访问
- [x] 4.2 确认 OpenAPI JSON 在 /v3/api-docs 可获取（YAML 格式在 /v3/api-docs.yaml 可用）
- [x] 4.3 确认所有 14 个接口在 Swagger UI 中正确展示

## 5. 前端代码生成环境搭建

- [ ] 5.1 安装 @hey-api/openapi-ts 和 @hey-api/client-axios 作为前端 devDependencies
- [ ] 5.2 创建 openapi-ts.config.ts 配置文件（配置 input 从后端获取、output 到 src/api/generated、使用 axios 插件）
- [ ] 5.3 在 package.json 中添加 `generate:api` 和 `generate:api:local` scripts

## 6. 生成前端代码

- [ ] 6.1 启动后端服务，执行 `npm run generate:api` 生成 TypeScript 类型和 SDK 代码
- [ ] 6.2 验证生成的类型定义覆盖所有 DTO（ProjectDTO、PaperDTO、AnalysisResult 等）
- [ ] 6.3 验证生成的 SDK 函数覆盖所有 13 个业务接口
- [ ] 6.4 配置生成的 axios client 与现有 interceptor 集成（自动解包 ApiResponse）

## 7. 前端业务代码迁移

- [ ] 7.1 替换 api/project.ts 中的手写函数为生成的 SDK 调用
- [ ] 7.2 替换 api/paper.ts 中的手写函数为生成的 SDK 调用
- [ ] 7.3 替换 api/analysis.ts 中的手写函数为生成的 SDK 调用
- [ ] 7.4 更新 stores/project.ts 使用新的 SDK 函数
- [ ] 7.5 更新 stores/paper.ts 使用新的 SDK 函数
- [ ] 7.6 更新 stores/analysis.ts 使用新的 SDK 函数
- [ ] 7.7 删除或标记废弃旧的 types/ 目录下的手写类型定义

## 8. 验证和构建

- [ ] 8.1 前端 `npx vite build` 构建成功，无类型错误
- [ ] 8.2 端到端测试：前端页面正确显示后端数据（项目列表、论文列表、分析结果）
- [ ] 8.3 确认生成的代码文件已提交到 git
