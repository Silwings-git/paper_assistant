## Why

当前项目状态机使用 `Project.status` 单一线性字段表示工作流阶段（CREATED → SEARCHING → SEARCHED → ANALYZING → ANALYZED），但用户实际操作中存在大量循环场景：

- 对搜索结果不满意，换关键词重新搜索
- 对分析结果不满意，增删论文后重新分析

现在的实现导致：
1. **数据混乱**：多次搜索时新旧论文混在一起，旧分析结果被覆盖
2. **UI 闪烁**：状态转换校验过于严格，重新操作时页面出现跳转/闪烁
3. **任务悬挂**：前一次任务未完成时触发新任务，导致多个 task 并行、前端显示混乱
4. **Project.status 语义过载**：既表示工作流阶段，又表示任务执行状态，职责不清

## What Changes

- **删除** `Project.status` 字段及 `ProjectStatus` 枚举
- **删除** `ProjectService.transitionStatus()` 及所有状态转换校验逻辑
- **新增** `TaskConcurrencyService`：按 task_type 互斥控制（同项目同类型只允许一个 RUNNING 任务）
- **修改** `PaperRetrievalService`：去掉对 `transitionStatus()` 的调用，论文保持追加去重
- **修改** `AnalysisOrchestrator`：去掉对 `transitionStatus()` 的调用，分析结果追加不覆盖
- **修改** `TaskStatusService`：新增 `hasActiveTask(projectId, taskType)` 和 `getVersionedResults(projectId, taskType)` 方法
- **修改前端**：所有基于 `project.status` 的 UI 判断改为基于 `task` 查询
- **BREAKING**: 数据库删除 `project` 表的 `status` 列
- **BREAKING**: API 响应中 `ProjectDTO.status` 字段移除

## Capabilities

### New Capabilities
- `task-concurrency`: 按任务类型互斥的并发控制机制，同一项目同一类型只允许一个活跃任务
- `project-task-state`: 项目活跃任务状态查询，替代原有的 Project.status 字段
- `analysis-versioning`: 分析结果版本化管理，支持保留和查看多次分析的历史结果

### Modified Capabilities
<!-- 无现有 spec 需要修改 -->

## Impact

- **后端**: `Project.java`, `ProjectStatus.java`(删除), `ProjectService.java`, `PaperRetrievalService.java`, `AnalysisOrchestrator.java`, `TaskStatusService.java`, `ProjectDTO.java`, `TaskStatusDTO.java`
- **数据库**: Flyway migration 删除 `project.status` 列
- **前端**: 所有使用 `project.status` 的组件和 store 需改写为 task 查询
- **OpenAPI**: ProjectDTO schema 变更，需重新生成前端代码
- **交叉影响**: 与 `html-mockup-ui`、`global-task-management`、`api-contract-automation` 三个 change 均有交叉
