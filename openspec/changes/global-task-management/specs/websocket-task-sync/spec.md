## ADDED Requirements

### Requirement: WebSocket task status subscription
系统 SHALL 使用 WebSocket/STOMP 订阅任务进度推送，替代 HTTP polling。

#### Scenario: Subscribe to task updates
- **WHEN** store 触发搜索或分析任务并获取 taskId
- **THEN** 通过 StompManager 订阅 `/topic/task/{taskId}`，接收实时更新

#### Scenario: Receive progress update
- **WHEN** WebSocket 推送任务进度消息（包含 status, progress, stage, message）
- **THEN** store 更新 currentTaskStatus，UI 响应显示最新进度

#### Scenario: Receive completion event
- **WHEN** WebSocket 推送任务完成消息（状态为 `SEARCHED`/`ANALYZED`）
- **THEN** store 停止订阅，刷新对应数据（论文列表或分析结果）

#### Scenario: Receive error/cancellation event
- **WHEN** WebSocket 推送错误消息或状态为 `FAILED`/`CANCELLED`
- **THEN** store 停止订阅，更新 UI 显示错误/取消状态

### Requirement: HTTP polling fallback
系统 SHALL 在 WebSocket 连接不可用时自动降级为 HTTP polling。

#### Scenario: WebSocket connection fails
- **WHEN** StompManager 连续 5 次 WebSocket 重连失败
- **THEN** 自动切换为 HTTP polling（间隔 3 秒）获取任务状态

#### Scenario: WebSocket recovers
- **WHEN** WebSocket 连接恢复
- **THEN** 重新使用 WebSocket 推送，停止 HTTP polling

### Requirement: Task restoration with WebSocket
系统 SHALL 在页面刷新时使用 WebSocket 恢复进行中任务的实时更新。

#### Scenario: Restore active search task on page mount
- **WHEN** 用户进入项目详情页，存在 `SEARCHING` 状态的活跃搜索任务
- **THEN** 通过 WebSocket 订阅该任务，恢复实时更新

#### Scenario: Restore active analysis task on page mount
- **WHEN** 用户进入项目详情页，存在 `ANALYZING` 状态的活跃分析任务
- **THEN** 通过 WebSocket 订阅该任务，恢复实时更新
