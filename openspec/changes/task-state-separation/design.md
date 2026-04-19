## Context

当前 Paper Assistant 使用 `Project.status` 枚举字段（CREATED/SEARCHING/SEARCHED/ANALYZING/ANALYZED）作为工作流状态的单一事实源。所有业务操作（搜索、分析）在开始和结束时通过 `ProjectService.transitionStatus()` 进行严格的状态转换校验。

问题：
1. 用户会重复执行搜索和分析操作，但当前状态机将这些视为"异常路径"而非正常操作
2. `Project.status` 既表示工作流阶段，又隐式表示"是否有任务在执行"，职责过载
3. 前端基于 `project.status` 做 UI 判断（如显示"搜索中"遮罩），但任务进度数据实际在 `TaskStatus` 表中，造成数据源分裂
4. 重复搜索时旧数据被新数据覆盖或混合，重复分析时旧结果被覆盖

约束：
- 三个 active change（html-mockup-ui, global-task-management, api-contract-automation）都依赖或影响状态相关代码
- `task_status` 表已存在且结构足够，无需新增表
- 数据库使用 PostgreSQL，Flyway 管理迁移

## Goals / Non-Goals

**Goals:**
- 删除 `Project.status` 字段，将状态管理完全迁移到 `TaskStatus` 表
- 实现按任务类型互斥的并发控制（同项目同类型只允许一个 RUNNING 任务）
- 论文数据支持追加去重（多次搜索结果累积）
- 分析结果支持版本化（多次分析结果保留）
- 前端通过查询活跃任务替代读取 `project.status`

**Non-Goals:**
- 不引入完整的"搜索版本"/"分析版本"切换 UI（后续迭代）
- 不改变 `TaskStatus` 表的核心结构
- 不修改 `ReproductionTask`、`Experiment` 等后续轮次的状态机
- 不实现跨项目的并发任务控制

## Decisions

### D1: Project.status 完全删除 vs 简化为 IDLE/BUSY

**Decision: 完全删除。**

理由：前端列表页展示"是否有任务在执行"可以通过 N+1 查询 `TaskStatus` 表解决。由于项目数量通常不多（< 50），N+1 的开销可接受。删除字段使数据模型更干净。

替代方案：保留 IDLE/BUSY 作为冗余字段。拒绝原因：冗余数据容易与实际状态不一致，增加维护成本。

### D2: 并发控制方式

**Decision: 按 task_type 互斥。** 同项目允许同时运行一个 SEARCH 和一个 ANALYSIS 任务，但不允许同时运行两个 SEARCH。

实现：新增 `TaskConcurrencyService.hasActiveTask(projectId, taskType)` 查询 `task_status` 表中 `status IN ('PENDING', 'RUNNING')` 的记录。

替代方案：全项目级别只允许一个 RUNNING 任务。拒绝原因：搜索和分析是独立的工作流，允许并行更合理。

### D3: 论文数据追加策略

**Decision: 保持现有的 `insertIgnore` 去重机制。** 多次搜索的论文累积在 `paper` 表中，通过 `source` + `source_id` 唯一键去重。

这意味着 Paper 表不再区分"第几次搜索"，论文来源信息通过 `source` 字段标识（arXiv / Semantic Scholar）。

替代方案：在 Paper 表增加 `search_task_id` 外键标记来源任务。拒绝原因：增加了复杂度，当前场景下用户只需知道"总共有哪些论文"，不需要按搜索批次分组。

### D4: 分析结果版本化

**Decision: 在 `analysis_result` 表中保持现有结构，不做版本字段扩展。** 每次分析创建新的 `analysis_result` 记录，通过 `create_time` 排序区分版本。

前端展示最新的分析结果，查询历史结果通过 `SELECT * FROM analysis_result WHERE project_id = ? ORDER BY create_time DESC`。

替代方案：增加 `version` 整数字段。拒绝原因：`create_time` 已足够排序，version 字段容易因并发写入产生冲突。

### D5: 失败回退策略

**Decision: 不再需要"失败回退 project.status"的逻辑。** 搜索或分析失败时，task_status 标记为 FAILED，用户可以直接重试。无需回退项目状态。

### D6: 前端数据获取模式

**Decision: 前端引入 `taskStore.getActiveTasks(projectId)` 方法，一次性获取项目的所有活跃任务。** 替代原来依赖 `project.status` 的判断。

组件层通过 `computed` 派生：
```ts
const isSearching = computed(() => tasks.value.find(t => t.taskType === 'SEARCH' && t.status === 'RUNNING'))
const hasAnalysisResults = computed(() => analysisResults.value.length > 0)
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| N+1 查询影响列表页性能 | 项目数量少时可接受；若增长可引入冗余字段或 JOIN 查询 |
| 三个 active change 的交叉冲突 | 建议先合并 `api-contract-automation`（确定 DTO schema），再合并本 change，最后处理 `html-mockup-ui` |
| 前端大量组件依赖 `project.status` | 系统性扫描所有使用点，统一替换为 task 查询模式 |
| 删除 `project.status` 是 BREAKING 变更 | 通过 Flyway migration 安全执行，前端同步更新 |
| 并发控制的边界情况（任务异常退出未更新 status） | 在 `TaskConcurrencyService` 中增加超时检测：超过 10 分钟的 RUNNING 任务视为异常，允许新任务启动 |
