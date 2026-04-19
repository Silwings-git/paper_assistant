## ADDED Requirements

### Requirement: Cancel task via API
系统 SHALL 提供 REST API 用于取消进行中的异步任务。后端 SHALL 将任务状态更新为 `CANCELLED`。

#### Scenario: Cancel a running search task
- **WHEN** 用户发送 `DELETE /api/v1/papers/tasks/{taskId}` 请求，且任务状态为 `SEARCHING`
- **THEN** 任务状态更新为 `CANCELLED`，返回成功响应

#### Scenario: Cancel a running analysis task
- **WHEN** 用户发送 `DELETE /api/v1/analysis/tasks/{taskId}` 请求，且任务状态为 `ANALYZING`
- **THEN** 任务状态更新为 `CANCELLED`，返回成功响应

#### Scenario: Cancel an already completed task
- **WHEN** 用户发送取消请求，但任务已处于终态（`SEARCHED`/`ANALYZED`/`FAILED`/`CANCELLED`）
- **THEN** 返回 400 错误，提示任务已结束

#### Scenario: Cancel a non-existent task
- **WHEN** 用户发送取消请求，但 taskId 不存在
- **THEN** 返回 404 错误

### Requirement: Check cancellation in async tasks
异步任务 SHALL 在执行过程中的检查点查询取消状态，如果已取消则提前退出。

#### Scenario: Search task checks cancellation after each paper
- **WHEN** 搜索任务每处理完一篇论文
- **THEN** 调用 `taskStatusService.isCancelled(taskId)` 检查，如已取消则终止执行并广播取消消息

#### Scenario: Analysis task checks cancellation between stages
- **WHEN** 分析任务完成阶段 1 后、开始阶段 2 前，以及完成阶段 2 后、开始阶段 3 前
- **THEN** 调用 `taskStatusService.isCancelled(taskId)` 检查，如已取消则终止执行并广播取消消息

### Requirement: Broadcast cancellation event
取消任务后，后端 SHALL 通过 WebSocket 广播取消事件。

#### Scenario: Broadcast cancellation to subscribers
- **WHEN** 任务被取消
- **THEN** 调用 `broadcastService.broadcastError(taskId, "任务已取消")`，WebSocket 订阅者收到通知
