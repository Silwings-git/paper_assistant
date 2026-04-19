## ADDED Requirements

### Requirement: 项目无 status 字段

`project` 表 MUST 不再包含 `status` 列，`Project` 实体和 `ProjectDTO` MUST 不再包含 `status` 字段。

#### Scenario: 数据库无 status 列
- **WHEN** 查询 `project` 表结构
- **THEN** `status` 列不存在

#### Scenario: API 响应无 status 字段
- **WHEN** 调用 `GET /api/v1/projects/{id}` 或 `GET /api/v1/projects`
- **THEN** 响应 JSON 中不包含 `status` 字段

### Requirement: 通过任务查询项目活跃状态

系统 MUST 提供通过查询 `task_status` 表获取项目活跃任务的能力，以替代原有的 `project.status` 判断。

#### Scenario: 查询项目活跃任务
- **WHEN** 调用 `GET /api/v1/projects/{id}/active-tasks`
- **THEN** 返回该项目所有状态为 PENDING 或 RUNNING 的任务列表

#### Scenario: 无活跃任务时返回空列表
- **WHEN** 项目 A 没有任何 PENDING/RUNNING 状态的任务
- **THEN** 活跃任务接口返回空数组 `[]`

### Requirement: 前端组件基于任务状态做 UI 判断

前端所有 UI 组件 MUST 通过查询 `TaskStore.activeTasks` 来判断是否显示进度条、加载遮罩等，不再依赖 `project.status`。

#### Scenario: 搜索进度展示
- **WHEN** `activeTasks` 中存在 `taskType === 'SEARCH'` 且 `status === 'RUNNING'` 的任务
- **THEN** 论文列表页显示进度条和搜索中提示

#### Scenario: 分析进度展示
- **WHEN** `activeTasks` 中存在 `taskType === 'ANALYSIS'` 且 `status === 'RUNNING'` 的任务
- **THEN** 分析报告页显示进度条和分析中提示

#### Scenario: 无活跃任务时显示操作按钮
- **WHEN** `activeTasks` 为空数组
- **THEN** 页面显示"搜索"和"分析"等操作按钮为可点击状态

### Requirement: 搜索论文追加去重

搜索任务完成后，新获取的论文 MUST 通过 `source` + `source_id` 去重后追加到 `paper` 表中，不删除已有论文。

#### Scenario: 第二次搜索结果追加
- **WHEN** 用户对项目 A 执行第二次搜索，返回 5 篇论文，其中 2 篇与已有论文 source+source_id 相同
- **THEN** 项目 A 的 paper 表新增 3 篇论文，原有论文不受影响

## REMOVED Requirements

### Requirement: 项目状态转换校验
**Reason**: `Project.status` 字段已删除，不再需要状态转换逻辑
**Migration**: 使用 `TaskConcurrencyService` 替代 `ProjectService.transitionStatus()` 控制任务启动条件

### Requirement: 任务完成后回退项目状态
**Reason**: 不再需要回退项目状态，任务失败时 task_status 本身已记录失败信息
**Migration**: 删除 `AnalysisOrchestrator` 中失败回退 `SEARCHED` 的逻辑，删除 `PaperRetrievalService` 中状态转换逻辑
