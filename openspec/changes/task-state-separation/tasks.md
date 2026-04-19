## 1. 数据库迁移

- [ ] 1.1 创建 Flyway migration: 删除 `project` 表的 `status` 列
- [ ] 1.2 验证 migration 不影响现有数据（paper、analysis_result 等）

## 2. 后端 - 实体与 DTO

- [ ] 2.1 从 `Project.java` 删除 `status` 字段及其 getter/setter
- [ ] 2.2 删除 `ProjectStatus.java` 枚举类
- [ ] 2.3 从 `ProjectDTO.java` 删除 `status` 字段
- [ ] 2.4 从 `CreateProjectRequest.java` 和 `UpdateProjectRequest.java` 移除 status 相关处理（如有）

## 3. 后端 - TaskConcurrencyService

- [ ] 3.1 创建 `TaskConcurrencyService.java`
- [ ] 3.2 实现 `hasActiveTask(projectId, taskType)` 方法：查询 task_status 表中 status IN ('PENDING','RUNNING') 的记录
- [ ] 3.3 实现 `detectStaleRunningTasks()` 方法：检测超过 10 分钟仍处于 RUNNING 的任务
- [ ] 3.4 在 `TaskStatusService.cancelTask()` 中确保取消后更新 status

## 4. 后端 - ProjectService 重构

- [ ] 4.1 删除 `transitionStatus()` 方法
- [ ] 4.2 删除 `isValidTransition()` 方法
- [ ] 4.3 删除 `VALID_STATUSES` 常量
- [ ] 4.4 从 `create()` 方法中删除 `project.setStatus(...)` 调用
- [ ] 4.5 从 `toDTO()` 方法中删除 `dto.setStatus(...)` 调用
- [ ] 4.6 从 `list()` 方法中删除 status 相关映射

## 5. 后端 - PaperRetrievalService 重构

- [ ] 5.1 在 `searchAsync()` 中移除 `projectService.transitionStatus(SEARCHING)` 调用
- [ ] 5.2 在 `searchAsync()` 中添加 `taskConcurrencyService.hasActiveTask()` 检查，有活跃任务时抛出异常
- [ ] 5.3 在 `doSearchAsync()` 中移除 `projectService.transitionStatus(SEARCHED)` 调用
- [ ] 5.4 确认 `persistPapers()` 使用 `insertIgnore` 实现追加去重（验证现有逻辑）

## 6. 后端 - AnalysisOrchestrator 重构

- [ ] 6.1 在 `analyzeAsync()` 中添加 `taskConcurrencyService.hasActiveTask()` 检查
- [ ] 6.2 在 `doAnalyzeAsync()` 中移除 `projectService.transitionStatus(ANALYZING)` 调用
- [ ] 6.3 在 `doAnalyzeAsync()` 中移除 `projectService.transitionStatus(ANALYZED)` 调用
- [ ] 6.4 在 `doAnalyzeAsync()` 的 catch 块中移除回退 `transitionStatus(SEARCHED)` 的逻辑
- [ ] 6.5 确认分析结果保存使用 `insert` 而非覆盖（验证现有逻辑）

## 7. 后端 - TaskStatusService 增强

- [ ] 7.1 新增 `hasActiveTask(projectId, taskType)` 公开方法
- [ ] 7.2 新增 `getVersionedResults(projectId, taskType)` 方法：按 create_time 降序返回历史结果
- [ ] 7.3 确保 `getLatestActiveTask()` 使用新的 status 判断逻辑（排除 FAILED/CANCELLED/COMPLETED）

## 8. 后端 - Controller 层

- [ ] 8.1 在 `ProjectController` 新增 `GET /projects/{id}/active-tasks` 端点
- [ ] 8.2 在 `AnalysisController` 新增 `GET /projects/{id}/analysis/history` 端点
- [ ] 8.3 在 `AnalysisController` 新增 `GET /projects/{id}/analysis/{resultId}` 端点
- [ ] 8.4 在 `AnalysisController` 新增 `DELETE /projects/{id}/analysis/{resultId}` 端点
- [ ] 8.5 为所有新端点添加 `@Operation`、`@Schema` OpenAPI 注解
- [ ] 8.6 更新 `GET /projects` 和 `GET /projects/{id}` 的响应 schema（移除 status 字段）

## 9. 前端 - API 重新生成

- [ ] 9.1 启动后端，运行 `npm run fetch:api` 获取最新 OpenAPI 规范
- [ ] 9.2 运行 `npm run generate:api` 重新生成 TypeScript SDK
- [ ] 9.3 检查 `types.gen.ts` 中 ProjectDTO 不再包含 status 字段
- [ ] 9.4 在 `src/api/index.ts` 中导出新增的 API 函数

## 10. 前端 - TaskStore 增强

- [ ] 10.1 在 analysis store 或新建 task store 中添加 `getActiveTasks(projectId)` 方法
- [ ] 10.2 添加 `getAnalysisHistory(projectId)` 方法
- [ ] 10.3 添加 `deleteAnalysisResult(resultId)` 方法

## 11. 前端 - ProjectDetail.vue 适配

- [ ] 11.1 移除所有基于 `project.status` 的条件判断
- [ ] 11.2 改用 `activeTasks` 判断是否显示进度条/加载状态
- [ ] 11.3 搜索按钮的可点击状态改为基于 `activeTasks` 中是否存在 SEARCH 任务
- [ ] 11.4 分析按钮的可点击状态改为基于 `activeTasks` 中是否存在 ANALYSIS 任务

## 12. 前端 - AnalysisReport.vue 适配

- [ ] 12.1 移除基于 `project.status` 的条件判断
- [ ] 12.2 分析按钮可点击状态改为基于 `activeTasks` 检查
- [ ] 12.3 默认展示最新分析结果
- [ ] 12.4 添加分析历史列表展示（后续迭代：版本切换 UI）

## 13. 前端 - Dashboard.vue 适配

- [ ] 13.1 移除项目卡片中的 status 标签展示
- [ ] 13.2 可选项：展示"有任务运行中"的图标替代原 status 文字

## 14. 测试与验证

- [ ] 14.1 手动测试：创建项目 → 搜索 → 再搜索 → 验证论文累积去重
- [ ] 14.2 手动测试：搜索完成后 → 分析 → 再分析 → 验证分析结果追加
- [ ] 14.3 手动测试：搜索中 → 再次搜索 → 验证被拒绝并提示
- [ ] 14.4 手动测试：分析中 → 再次分析 → 验证被拒绝并提示
- [ ] 14.5 手动测试：分析中 → 搜索 → 验证允许并行
- [ ] 14.6 手动测试：分析失败 → 重新分析 → 验证允许
- [ ] 14.7 验证所有新 API 端点在 Swagger UI 中可正常调用
