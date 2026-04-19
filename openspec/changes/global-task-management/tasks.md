## 1. 后端 — 任务取消

- [x] 1.1 TaskStatusService 新增 `cancelTask(taskId)` 方法：更新状态为 CANCELLED
- [x] 1.2 TaskStatusService 新增 `isCancelled(taskId)` 方法：返回 boolean
- [x] 1.3 PaperController 新增 `DELETE /tasks/{taskId}` 接口，添加 Swagger 注解
- [x] 1.4 AnalysisController 新增 `DELETE /tasks/{taskId}` 接口，添加 Swagger 注解
- [x] 1.5 PaperRetrievalService.doSearchAsync 中在 persistPapers 后添加取消检查
- [x] 1.6 AnalysisOrchestrator.doAnalyzeAsync 中在阶段 1 后、阶段 2 后添加取消检查

## 2. 后端 — 重启验证 & 前端 API 生成

- [ ] 2.1 启动后端，访问 Swagger 验证取消接口正常
- [ ] 2.2 前端执行 `npm run fetch:api` 获取最新 OpenAPI 规范
- [ ] 2.3 前端执行 `npm run generate:api` 生成 TypeScript SDK

## 3. 前端 — WebSocket 任务订阅

- [x] 3.1 StompManager 新增 `subscribeTask(taskId, callbacks)` 方法，支持 onProgress/onComplete/onError 回调
- [x] 3.2 StompManager 新增 `unsubscribeTask(taskId)` 方法
- [x] 3.3 修改 stores/paper.ts：search() 改用 WebSocket 订阅替代 setInterval polling
- [x] 3.4 修改 stores/analysis.ts：trigger() 改用 WebSocket 订阅替代 setInterval polling
- [x] 3.5 修复 analysis store 恢复任务时不重启 polling 的 bug：onMounted 检测到 ANALYZING 状态后应重新订阅 WebSocket

## 4. 前端 — 全局 taskManager

- [x] 4.1 创建 stores/taskManager.ts：activeTasks Map、register(projectId)、unregister(projectId)
- [x] 4.2 taskManager 实现 cancelTask(taskId, taskType) 方法
- [x] 4.3 新增 types/analysis.ts 中 CANCELLED 状态支持
- [x] 4.4 修改 stores/paper.ts：search() 中遇到 CANCELLED 状态时停止搜索

## 5. 前端 — 全局任务横幅

- [x] 5.1 创建 components/GlobalTaskBanner.vue：全局横幅组件
- [x] 5.2 修改 App.vue：引入 GlobalTaskBanner 组件
- [x] 5.3 GlobalTaskBanner 中显示任务进度条、阶段描述、取消按钮
- [x] 5.4 点击取消按钮调用 taskManager.cancelTask，横幅响应式更新

## 6. 前端 — 项目详情页接入

- [x] 6.1 ProjectDetail.vue onMounted 中调用 taskManager.register(projectId)
- [x] 6.2 ProjectDetail.vue onUnmounted 中调用 taskManager.unregister(projectId)
- [x] 6.3 根据 active task 状态动态禁用搜索/分析按钮（覆盖静态 projectStatus 判断）
- [x] 6.4 项目详情页内进度条新增"取消"按钮
- [x] 6.5 取消操作后更新 UI 状态并刷新论文列表（如适用）
