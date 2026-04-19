## Why

研究者需要一个工具来快速检索某个研究方向的最新论文进展，并通过 LLM 分析找出可创新的切入点。当前这个过程完全手动：在 arXiv 上浏览、逐篇阅读、凭经验判断研究空白。论文助手的 Round 1 MVP 将"检索 → 分析 → 选择 base 论文"这条核心链路自动化，让用户能在几分钟内完成原本需要几天的文献调研工作。

## What Changes

- 新增论文检索能力：支持通过关键词检索 arXiv 和 Semantic Scholar，返回论文列表及元数据（标题、摘要、作者、引用数、是否有开源代码等）
- 新增 LLM 创新分析能力：对检索结果进行批量分析，输出研究空白、推荐 base 论文列表及每个论文的可创新点建议
- 新增项目管理能力：创建/查看/删除研究项目，每个项目绑定一个研究方向、检索结果和分析报告
- 新增基础 Web 前端：提供论文搜索、结果浏览、分析报告查看的交互界面
- 新增 LLM 集成层：对接阿里百炼 (DashScope) API，支持 prompt 模板管理和模型切换
- 新增 PostgreSQL 数据持久化：存储项目、论文、分析结果等核心数据

## Capabilities

### New Capabilities

- `paper-retrieval`: 论文检索，支持 arXiv 和 Semantic Scholar 双源检索，结果聚合和排序
- `innovation-analysis`: LLM 驱动的创新机会分析，输出研究空白和 base 论文推荐
- `project-management`: 研究项目的 CRUD，绑定研究方向、检索结果、分析报告
- `llm-integration`: LLM provider 抽象层，当前支持 DashScope（阿里百炼），预留多 provider 扩展
- `frontend-mvp`: 最小可用前端，覆盖论文搜索、浏览、分析查看的核心交互

### Modified Capabilities

<!-- 无已有 specs -->

## Impact

- **新建模块**: `paper-assistant-web` (Spring Boot 单模块应用) 包含 retrieval / analysis / llm / project 四个 service 包 + scheduler 定时任务包
- **新建模块**: `paper-assistant-frontend` (Vue 3 + TypeScript)
- **新增依赖**: Spring Boot, LangChain4j, DashScope SDK, PostgreSQL, Redis, Vue 3, Element Plus
- **无现有 API 影响**: 项目从零开始，无 breaking changes
- **Out of Scope (后续 Round)**: Rust 执行引擎、Docker 容器化、代码复现、实验设计、论文撰写、多用户系统、分布式调度 (XXL-JOB 已预留接口)
