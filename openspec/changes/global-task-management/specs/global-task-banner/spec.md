## ADDED Requirements

### Requirement: Global task banner display
系统 SHALL 在应用顶部（App.vue）渲染全局任务横幅，显示所有进行中任务的实时进度。

#### Scenario: Show active search task
- **WHEN** 存在状态为 `SEARCHING` 的任务
- **THEN** 横幅显示搜索图标、进度百分比、阶段描述和取消按钮

#### Scenario: Show active analysis task
- **WHEN** 存在状态为 `ANALYZING` 的任务
- **THEN** 横幅显示分析图标、进度百分比、阶段描述和取消按钮

#### Scenario: Multiple tasks visible
- **WHEN** 同时存在多个进行中任务
- **THEN** 横幅依次显示每个任务的进度条

#### Scenario: No active tasks
- **WHEN** 没有任何进行中任务
- **THEN** 横幅不渲染（不占用布局空间）

### Requirement: Global taskManager store
系统 SHALL 提供独立 Pinia store 用于管理全局活跃任务集合。

#### Scenario: Register project tasks
- **WHEN** 用户进入项目详情页调用 `taskManager.register(projectId)`
- **THEN** taskManager 查询该项目的 active search 和 active analysis 任务，加入 activeTasks Map

#### Scenario: Unregister project tasks
- **WHEN** 用户离开项目详情页调用 `taskManager.unregister(projectId)`
- **THEN** taskManager 从 activeTasks Map 中移除该项目的任务，并取消对应的 WebSocket 订阅

#### Scenario: Task state update from WebSocket
- **WHEN** WebSocket 推送任务状态更新
- **THEN** taskManager 更新 activeTasks Map 中对应任务的状态，横幅自动响应

#### Scenario: Cancel task from banner
- **WHEN** 用户在横幅中点击"取消"按钮
- **THEN** 调用取消 API，从 activeTasks 中移除该任务，横幅更新

### Requirement: Project status overrides from active tasks
系统 SHALL 根据 active 任务状态动态禁用项目操作，优先于 projectStatus 的静态判断。

#### Scenario: Disable search when searching task exists
- **WHEN** 项目存在 `SEARCHING` 状态的任务
- **THEN** 项目详情页的搜索按钮和搜索输入框被禁用

#### Scenario: Disable analysis when analyzing task exists
- **WHEN** 项目存在 `ANALYZING` 状态的任务
- **THEN** 项目详情页的触发分析按钮被禁用

#### Scenario: Cancel task re-enables actions
- **WHEN** 用户取消任务后
- **THEN** 相关操作按钮恢复可用状态
