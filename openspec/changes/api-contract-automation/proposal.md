## Why

前后端接口目前没有任何契约文档。后端 14 个接口无 Swagger 标注，前端手写 API 调用层猜测参数格式和返回结构，导致多次联调问题（如 ApiResponse 解包、分页字段不对齐、query param 与 body 混淆）。引入 Swagger/OpenAPI 作为接口契约，并自动生成前端 TypeScript 类型和 SDK 代码，从根本上消除前后端接口不一致问题。

## What Changes

- 后端集成 springdoc-openapi，所有 Controller、DTO、Request 添加完整的 OpenAPI 注解（@Tag、@Operation、@Schema）
- 启动后可通过 Swagger UI 浏览和测试所有接口
- 导出 OpenAPI 3.1 JSON 规范到前端项目
- 前端使用 @hey-api/openapi-ts 自动生成 TypeScript 类型定义和 API SDK 代码
- 替换现有手写的 api/*.ts 和 types/*.ts 为自动生成的代码
- 前端 stores 层调整为调用生成的 SDK 函数

## Capabilities

### New Capabilities
- `swagger-api-docs`: 后端所有 REST 接口的 OpenAPI 3.1 注解和 Swagger UI 支持
- `frontend-codegen`: 前端基于 OpenAPI 规范自动生成 TypeScript 类型和 API SDK 代码

### Modified Capabilities
<!-- 无现有 spec，全部为新增 -->

## Impact

- **后端**: `paper-assistant-web/pom.xml` 新增 springdoc 依赖；所有 4 个 Controller 文件需添加注解；所有 DTO/Request 类需添加 @Schema；`application-dev.yml` 新增 springdoc 配置
- **前端**: `api/project.ts`、`api/paper.ts`、`api/analysis.ts` 将被自动生成的 SDK 替代；`types/` 目录下的类型定义将被自动生成替代；`stores/` 中的 3 个 store 需调整调用方式
- **构建流程**: 前端构建前需先生成 OpenAPI 代码（新增 npm script）
