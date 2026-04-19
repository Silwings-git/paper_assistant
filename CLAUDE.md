# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

## 语言要求

**本项目中的所有文档必须使用中文书写**，包括但不限于：
- 本文件 (CLAUDE.md)
- OpenSpec 相关文档（`openspec/` 目录下的所有文件）
- Claude 生成的所有文档
- README、设计文档、规范文档、注释等

如遇英文技术术语，可保留英文原文，但说明文字必须使用中文。

## 项目概述

**Paper Assistant** — 一个 AI 驱动的研究论文工具，帮助研究人员发现最新论文、分析创新机会、复现论文代码、设计和运行实验，以及生成 SCI 论文。

## 技术栈

| 层级 | 技术 |
|-------|-----------|
| 后端 | Java (Spring Boot) |
| 执行引擎 | Rust (Tonic gRPC) — 后续轮次 |
| 数据库 | PostgreSQL |
| 缓存 | Redis — 检索结果缓存、限流队列 |
| 前端 | Vue 3 + TypeScript |
| LLM | DashScope (阿里百炼)，支持多提供者抽象 |
| 容器 | Docker (用于论文复现) — 后续轮次 |

## 模块结构 (规划中)

```
paper-assistant/
├── pom.xml                       # 父 POM (多模块)
├── paper-assistant-web/          # Spring Boot 后端 (单模块应用)
│   ├── controller/
│   ├── service/
│   │   ├── retrieval/            # arXiv + Semantic Scholar 获取
│   │   ├── analysis/             # LLM 驱动的创新分析
│   │   ├── experiment/           # 实验设计 (后续)
│   │   ├── writing/              # 论文生成 (后续)
│   │   ├── llm/                  # LLM 提供者抽象
│   │   └── execution/            # Rust gRPC 客户端 (后续)
│   ├── scheduler/                # 定时任务包 (@Scheduled + HTTP API)
│   └── config/
├── paper-assistant-executor/     # Rust 守护进程 (后续轮次)
├── paper-assistant-proto/        # gRPC proto 定义 (后续)
└── paper-assistant-frontend/     # Vue 3 + TypeScript 单页应用
```

**定时任务**: `scheduler` 包位于 web 模块内，`@Scheduled` 通过 Spring DI 调用业务执行器。保留 `POST /api/v1/scheduler/{job-name}/execute` HTTP API 供未来 XXL-JOB 等分布式调度远程调用。

## 当前状态

Round 1 MVP 开发已完成，包括：论文检索、LLM 分析、项目管理、前后端完整实现、DashScope 集成、Swagger/OpenAPI 文档化、前端 API 自动生成。

## 前后端接口工作流

**后端是 API 的单一事实源**，前端代码通过 OpenAPI 规范自动生成。

### 后端新增/修改接口

1. 在 Controller 方法上添加 `@Operation`、`@Parameter`、`@ApiResponses`、`@Tag` 注解
2. 在 DTO 类字段上添加 `@Schema` 注解
3. 启动后端，访问 `http://localhost:8080/v3/api-docs.yaml` 确认规范已更新

### 前端重新生成代码

```bash
cd paper-assistant-frontend
npm run fetch:api        # 下载最新的 OpenAPI 规范到 openapi/openapi.yaml
npm run generate:api     # 使用 @hey-api/openapi-ts 生成 src/api/generated/
```

### 前端适配步骤

1. 检查 `src/api/generated/types.gen.ts` 中的新增/变更类型
2. 在 `src/api/index.ts` 中导出新的 API 函数（如需）
3. 在 `src/types/` 对应文件中添加类型定义和 `toXXX()` 转换函数
4. 在对应 store 中调用新 API 并使用转换函数

### 前端 API 分层结构

```
paper-assistant-frontend/src/api/
├── generated/              # 自动生成，不要手动修改
│   ├── client.gen.ts       # axios 客户端实例
│   ├── sdk.gen.ts          # 各端点的 SDK 函数（URL 已包含 /api/v1 前缀）
│   ├── types.gen.ts        # TypeScript 类型定义
│   └── ...
└── index.ts                # 手写：统一导出 + ApiResponse 拦截器（baseURL 必须设为空字符串）
```

### 前端类型适配

`src/types/` 中的文件维护旧类型定义和转换函数，用于向后兼容组件层：

```
src/types/
├── project.ts  → toProjectDTO(), toProjectDtoList()
├── paper.ts    → toPaperDTO(), toPaperDTOList()
└── analysis.ts → toAnalysisResultDTO(), toTaskStatusDTO()
```

**规则**：不要手动修改 `src/api/generated/` 下的任何文件。所有类型差异通过 `src/types/` 中的转换函数桥接。

## 架构决策

- **Java ↔ Rust 通信**: gRPC (Rust 作为独立守护进程运行，使用 Tonic)
- **Rust 守护进程部署**: 独立进程，可独立部署到 GPU 机器
- **LLM 抽象**: 提供者无关接口，DashScope 优先，后续支持 OpenAI/Anthropic
- **环境隔离**: Docker 容器 (而非 conda) 用于论文复现，配合 NVIDIA Container Toolkit 进行 GPU 直通
- **数据持久化**: PostgreSQL 存储所有业务数据
- **前端框架**: Vue 3 + Element Plus

## 架构文档

完整的全局架构决策（跨所有开发轮次）见 `docs/architecture.md`，包含：
- 整体部署架构（Java + Rust + Vue）
- 核心数据模型
- 完整工作流状态机
- Java ↔ Rust gRPC 接口定义
- Rust Daemon 内部架构
- Docker 容器化方案
- LLM 使用策略
- 轮次规划

Round 1 的具体技术决策见 `openspec/changes/round-1-mvp/design.md`。

## 其他文档

`docs/开发复盘/` 目录包含 OpenSpec 工具的使用指南和本次开发的复盘记录，与本项目业务无关，开发时无需参考。

## 命令

### 后端

```bash
cd paper-assistant-web
# 开发环境启动（需设置 spring.profiles.active=dev）
./mvnw spring-boot:run
# 打包
./mvnw clean package -DskipTests
```

### 前端

```bash
cd paper-assistant-frontend
npm run dev          # 开发服务器
npm run build        # 生产构建
npm run fetch:api    # 从后端下载最新 OpenAPI 规范
npm run generate:api # 从规范生成 TypeScript SDK
```
