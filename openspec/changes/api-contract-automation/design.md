## Context

当前 Paper Assistant 项目有 14 个 REST API 接口，分布在 4 个 Controller 中。所有接口均无任何 OpenAPI/Swagger 注解，前端完全靠猜测和试错来调用接口。这导致了多次联调问题：ApiResponse 包装层解包不一致、分页字段不对齐、query param 与 body 参数混淆等。

技术栈：Spring Boot 3.4.4 + Java 21 + Vue 3 + TypeScript。前端已有 axios interceptor 自动解包 ApiResponse，后续生成的 SDK 需要与此行为保持一致。

## Goals / Non-Goals

**Goals:**
- 后端所有接口具备完整的 OpenAPI 3.1 注解，启动后自动生成 API 文档
- 前端可通过 OpenAPI JSON 自动生成 TypeScript 类型和 API SDK 代码
- 消除前后端接口不一致的联调问题
- 建立可持续维护的接口契约流程（后端改接口 → 导出 OpenAPI → 重新生成前端代码）

**Non-Goals:**
- 不改变现有 API 的行为或 URL 结构
- 不引入 Springfox（已过时，不兼容 Spring Boot 3）
- 不改变 ApiResponse 统一响应包装格式
- 不修改后端业务逻辑代码

## Decisions

### 1. 使用 springdoc-openapi v2 (starter-webmvc-ui)

**决策**: 添加 `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.x` 依赖

**理由**: springdoc 是 Spring Boot 3.x 官方推荐的 OpenAPI 方案，自动扫描 Controller 生成文档，无需手动编写 YAML。

**替代方案**: 手动编写 OpenAPI YAML（维护成本高，容易与代码脱节）

### 2. OpenAPI 文档通过注解生成，不写独立 YAML

**决策**: 所有接口文档通过 @Tag、@Operation、@Schema 注解在代码中维护

**理由**: 注解与代码同文件，不会出现文档与实现不一致的问题。springdoc 在启动时自动扫描生成 JSON。

### 3. 前端使用 @hey-api/openapi-ts + @hey-api/client-axios 生成代码

**决策**: 使用 hey-api 的 openapi-ts 工具链，配合 axios client 插件

**理由**: 支持 OpenAPI 3.1，生成的代码基于 TypeScript 类型，自带 axios 集成，与项目现有 axios 体系兼容。

**替代方案**: openapi-typescript（只生成类型，不生成 SDK 函数）；swagger-codegen（生成代码臃肿，风格老旧）

### 4. 前端生成的 SDK 保留现有 interceptor 解包行为

**决策**: 在生成的 axios client 配置中复用现有的 request interceptor（自动解包 ApiResponse），生成的 SDK 函数返回类型直接使用解包后的 T 类型

**理由**: 保持前端业务代码的体验一致，不需要在每个调用处手动解包。通过 openapi-ts 的自定义配置实现。

### 5. OpenAPI JSON 导出作为前端构建的前置步骤

**决策**: 在前端 package.json 中添加 `generate:api` script，从运行中的后端获取 `/v3/api-docs` JSON，然后执行代码生成

**理由**: 确保前端生成的代码始终与后端实际接口一致。手动复制 JSON 容易遗漏。

### 6. Swagger UI 仅在 dev 环境启用

**决策**: 在 application-dev.yml 中启用 springdoc，prod 环境默认关闭

**理由**: Swagger UI 暴露接口信息，生产环境不应开放。

## Risks / Trade-offs

- **[风险]** springdoc 自动生成的 OpenAPI 可能不完美反映 ApiResponse 包装后的实际返回类型 → **缓解**: 在 DTO 层面添加完整 @Schema 注解，必要时使用 @ApiResponse 标注包装结构
- **[风险]** openapi-ts 生成的代码风格可能与现有代码不匹配 → **缓解**: 生成后手动微调一次，后续以生成为准
- **[风险]** 后端接口变更后忘记重新生成前端代码 → **缓解**: 在 CI 中添加契约检查步骤（后续优化）
- **[权衡]** @Schema 注解会增加大量样板代码 → 接受此代价，换取自动生成文档和类型的完整性
