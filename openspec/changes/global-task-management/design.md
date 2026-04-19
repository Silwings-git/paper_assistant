## Context

当前项目使用 HTTP polling（`setInterval` 每 2 秒）获取异步任务状态，后端已有 WebSocket/STOMP 基础设施（`WebSocketBroadcastService` 在每个阶段推送进度到 `/topic/task/{taskId}`），但前端 store 未消费。任务取消功能完全缺失。任务状态横幅仅存在于 `ProjectDetail.vue`，离开页面即不可见。

约束：
- 后端使用 `@Async("retrievalExecutor")` 和 `@Async("analysisExecutor")` 执行异步任务，线程池管理
- 前端已有 `StompManager`（`src/utils/websocket.ts`）并已在 `main.ts` 中连接
- 前端已有 `GlobalTaskBanner` 的占位引用但未实现

## Goals / Non-Goals

**Goals:**
- 用户可在任意页面取消进行中的搜索/分析任务
- 全局横幅实时显示所有进行中任务的进度，跨页面可见
- 前端任务状态更新使用 WebSocket 推送，HTTP polling 作为 fallback
- 后端异步任务在阶段间检查取消信号，提前退出释放资源

**Non-Goals:**
- 不修改 WebSocket 后端架构（`WebSocketBroadcastService` 保持不变）
- 不支持批量取消（一次只能取消一个任务）
- 不实现任务暂停/恢复功能
- 不做历史任务列表页（仅关注当前 active 任务）

## Decisions

### D1: 取消机制 — DB 标记 + 阶段检查 vs `CompletableFuture.cancel()`

**决策**: DB 标记 + 阶段检查

**理由**: Java `@Async` 线程无法安全中断，`Thread.interrupt()` 可能破坏 LLM 调用状态。改为在 `TaskStatusService` 中新增 `isCancelled(taskId)` 方法，`PaperRetrievalService` 在每篇论文处理后、`AnalysisOrchestrator` 在每个阶段结束后检查。

**备选方案**:
- `CompletableFuture.cancel(true)` — 会抛出 `InterruptedException`，但 LLM 调用不可中断，可能泄漏资源
- 使用 `CountDownLatch` + `Thread.interrupt()` — 过于复杂，与现有架构不兼容

### D2: WebSocket 接入方式 — 修改现有 StompManager vs 重写

**决策**: 修改现有 `StompManager`，新增类型安全的任务订阅 API

**理由**: 现有 `StompManager` 已有连接管理、重连、自动 fallback polling 的完整实现。只需新增 `subscribeTask(taskId, callbacks)` 和 `unsubscribeTask(taskId)` 方法。

```typescript
// 新增 API
stompManager.subscribeTask(taskId, {
  onProgress: (data: TaskStatusDTO) => void,
  onComplete: (data: TaskStatusDTO) => void,
  onError: (error: string) => void
})
stompManager.unsubscribeTask(taskId)
```

### D3: 全局 taskManager — 独立 store vs 嵌入现有 store

**决策**: 独立 `stores/taskManager.ts`

**理由**: `paperStore` 和 `analysisStore` 职责单一，分别管理搜索和分析的数据。全局横幅需要同时管理两种任务的状态，独立 store 避免循环依赖。taskManager 仅维护 `activeTasks: Map<projectId, TaskInfo[]>` 和 WebSocket 订阅管理。

**架构**:
```
┌──────────────────────────────────────────┐
│              App.vue                      │
│  ┌────────────────────────────────────┐  │
│  │     GlobalTaskBanner.vue           │  │
│  │  订阅 taskManager.activeTasks      │  │
│  └────────────────────────────────────┘  │
│                                           │
│  ┌────────────────────────────────────┐  │
│  │  路由视图 (ProjectDetail 等)        │  │
│  │  onMounted → taskManager.register  │  │
│  │  onUnmounted → taskManager.unregister│ │
│  └────────────────────────────────────┘  │
│                                           │
│  stores/taskManager.ts                    │
│  ├── activeTasks: Map                     │
│  ├── register(projectId)                  │
│  ├── unregister(projectId)                │
│  ├── cancelTask(taskId)                   │
│  └── WebSocket subscription management    │
└──────────────────────────────────────────┘
```

### D4: CANCELLED 状态 — 新增 vs 复用 FAILED

**决策**: 新增 `CANCELLED` 状态

**理由**: 用户主动取消与系统失败语义不同，前端需要区分展示（"已取消" vs "失败"）。后端 `TaskStatusEntity.status` 是字符串字段，无需改表结构。

### D5: 取消后列表刷新策略

**决策**:
- 搜索取消 → **不刷新**论文列表（已搜到的部分结果已在 DB 中）
- 分析取消 → **不刷新**分析结果（已完成的部分分析已保存）

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| `@Async` 线程无法真正中断，取消后资源仍消耗到阶段结束 | 搜索任务每处理一篇论文即检查；分析任务每阶段结束即检查。最长延迟为一个阶段的耗时 |
| WebSocket 断连时 fallback polling 增加服务器负载 | StompManager 已有 3 秒间隔 fallback，且仅在有 active 任务时启动 |
| 多 tab 打开时多个 WebSocket 连接重复订阅 | 当前不做跨 tab 协调，每个 tab 独立连接。MVP 可接受 |
| taskManager 注册/ unregister 不对导致内存泄漏 | `onUnmounted` 中必须调用 `unregister`，Vue 组件销毁时自动触发 |
