## Why

当前项目的异步任务（搜索/分析）缺乏两个关键能力：

1. **无法取消进行中任务** — 用户发起搜索或分析后，无法中途停止，只能等待完成或刷新页面
2. **任务状态仅限项目详情页可见** — 用户离开项目页面后，无法感知后台任务进度

同时，前端使用 HTTP polling（每 2 秒轮询）获取任务状态，而后端已有 WebSocket/STOMP 基础设施但未被消费，应切换为 WebSocket 实时推送以降低服务器负载并提升用户体验。

## What Changes

- 新增任务取消能力：用户可在任意页面取消进行中任务
- 新增全局任务状态横幅：在 App 顶部显示所有进行中任务的进度，跨页面可见
- 前端任务状态更新从 HTTP polling 切换为 WebSocket 实时推送（保留 polling 作为 fallback）
- 新增 `CANCELLED` 任务状态
- 后端异步任务支持取消信号，在各阶段间检查并提前退出

## Capabilities

### New Capabilities

- `task-cancellation`: 任务取消能力，包含后端取消接口、取消状态传播、以及前端取消交互
- `global-task-banner`: 全局任务状态展示，包含全局 taskManager store、顶部横幅组件、WebSocket 任务订阅
- `websocket-task-sync`: WebSocket 实时任务状态推送，前端 store 通过 WebSocket 接收任务进度替代 polling

### Modified Capabilities

<!-- No existing specs to modify -->

## Impact

**后端**:
- `PaperController` — 新增 `DELETE /tasks/{taskId}` 接口
- `AnalysisController` — 新增 `DELETE /tasks/{taskId}` 接口
- `TaskStatusService` — 新增 `cancelTask()` 和 `isCancelled()` 方法
- `PaperRetrievalService` — 搜索循环中增加取消检查
- `AnalysisOrchestrator` — 阶段间增加取消检查

**前端**:
- 新增 `stores/taskManager.ts` — 全局任务管理 store
- 新增 `components/GlobalTaskBanner.vue` — 顶部全局任务横幅
- 修改 `stores/paper.ts` — 用 WebSocket 替代 polling
- 修改 `stores/analysis.ts` — 用 WebSocket 替代 polling + 修复恢复 polling bug
- 修改 `ProjectDetail.vue` — 接入 taskManager + 新增取消按钮
- 修改 `App.vue` — 挂载 GlobalTaskBanner
- 修改 `types/analysis.ts` — 新增 CANCELLED 状态
- 通过 `fetch:api` + `generate:api` 重新生成 API 类型定义
