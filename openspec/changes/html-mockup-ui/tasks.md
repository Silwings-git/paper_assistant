## 1. 全局 CSS 变量系统

- [x] 1.1 将设计稿中的 CSS 变量提取到 App.vue 的全局 :root 中
- [x] 1.2 创建共享 CSS 类（btn、badge、modal 等）到 assets/design-tokens.css

## 2. Dashboard 页面重写

- [x] 2.1 验证 API 层 `getProjects()` / `createProject()` / `deleteProject()` 的数据格式和字段映射（ProjectDto → ProjectDTO）
- [x] 2.2 重写 Dashboard.vue 模板：用原生 div + CSS 替代 el-card、el-dialog、el-button、el-empty
- [x] 2.3 添加项目创建弹窗（modal overlay + form）的 Vue 响应式状态，对接 `store.create()`
- [x] 2.4 添加删除确认弹窗，对接 `store.remove()`
- [x] 2.5 复制设计稿 01-dashboard.html 的 CSS 到 Dashboard.vue scoped 样式
- [ ] 2.6 验证：创建/删除项目后数据实时更新

## 3. ProjectDetail 页面重写

- [x] 3.1 验证 API 层 `getPapersApi()` / `searchPapersApi()` / `getPaperTaskStatusApi()` / `setProjectBasePaper()` 的数据格式和字段映射
- [x] 3.2 重写 ProjectDetail.vue 模板：用原生 table 替代 el-table，绑定 `paperStore.papers`
- [x] 3.3 实现表格工具栏（选中计数 + 排序标签页），排序切换调用 `paperStore.fetchList()`
- [x] 3.4 实现论文详情抽屉（drawer overlay + 滑动面板），点击行显示论文详情
- [x] 3.5 实现 Base 论文选择器弹窗，单选后调用 `projectStore.setBase()`
- [x] 3.6 实现任务进度条组件（细轨道 + 百分比 + 阶段文字），绑定 `paperStore.currentTaskStatus`
- [x] 3.7 实现分页控件（页码按钮 + 前后翻页 + 总数信息），切换页码调用 `paperStore.fetchList()`
- [x] 3.8 实现搜索功能，绑定 `paperStore.search()` 及 loading 状态
- [x] 3.9 实现警告横幅（黄色背景 + 图标 + 提示文字）
- [x] 3.10 实现「触发分析」按钮，对接 `analysisStore.trigger()`
- [x] 3.11 复制设计稿 02-project-detail.html 的 CSS 到 ProjectDetail.vue scoped 样式
- [x] 3.12 【异步】保留 onMounted 恢复逻辑：检查 currentTaskId → fetchTaskStatus → 如果 SEARCHING 重新轮询
- [x] 3.13 【异步】保留 onUnmounted 清理逻辑：调用 stopSearchPolling() + stopAnalyzePolling()
- [x] 3.14 【异步】保留 watch 监听 paperStore.currentTaskStatus 和 analysisStore.currentTaskStatus，更新 partialFailure / analyzing 状态
- [x] 3.15 【异步】进度条精确映射：progress → width%，message → stage text，status → 颜色/状态
- [ ] 3.16 验证：搜索→轮询→表格更新→选择 Base 论文→触发分析的完整流程

## 4. AnalysisReport 页面重写

- [x] 4.1 验证 API 层 `triggerAnalysisApi()` / `getAnalysisResultApi()` / `getAnalysisTaskStatusApi()` 的数据格式和字段映射
- [x] 4.2 验证 `AnalysisResultDTO` 中 `gaps` / `basePapers` / `innovationPts` 的 JSON 解析结果与设计稿字段对应关系
- [x] 4.3 重写 AnalysisReport.vue 模板：替代 el-card、el-progress、el-tag、el-skeleton
- [x] 4.4 实现报告头部（标题 + 关键词 + 状态徽章）
- [x] 4.5 实现研究空白网格（2×2 gap-grid + gap-card），渲染 category/description/evidence/supportingPapers
- [x] 4.6 实现 Base 论文推荐列表（纵向 rec-card + 排名徽章），渲染 title/authors/reason/innovationDirection
- [x] 4.7 实现创新点网格（3 列 innovation-grid + 难度彩色顶条），渲染 description/difficulty/contributionType
- [x] 4.8 复制设计稿 03-analysis-report.html 的 CSS 到 AnalysisReport.vue scoped 样式
- [x] 4.9 【异步】保留 onMounted 恢复逻辑：检查 analysisStore.currentTaskId → fetchTaskStatus → 如果 ANALYZING 重新触发轮询
- [x] 4.10 【异步】保留 onUnmounted 清理：调用 stopAnalyzePolling()
- [x] 4.11 【异步】保留 watch 监听 analysisStore.currentTaskStatus，动态更新 analyzing 按钮状态和 taskProgress/taskStatusType/taskMessage
- [x] 4.12 【异步】分析结果加载完成后，自动更新按钮状态并停止轮询
- [ ] 4.13 验证：触发分析→轮询进度→结果渲染的完整流程

## 5. 清理与验证

- [x] 5.1 移除三个页面中不再使用的 Element Plus 组件导入（el-card、el-table、el-dialog、el-pagination、el-progress、el-alert、el-skeleton、el-empty、el-tag、el-form、el-form-item）
- [x] 5.2 保留 ElMessage 等工具类组件的引用
- [ ] 5.3 【异步】验证轮询生命周期：onMounted 恢复 → 轮询中 → 终态停止 → onUnmounted 清理
- [ ] 5.4 【异步】验证进度条实时更新：触发任务后进度条在 2 秒内开始变化，终态后停止
- [ ] 5.5 验证三个页面在开发模式下的完整数据流：创建项目→搜索论文→选择 Base→触发分析→查看报告
- [ ] 5.6 验证响应式布局（1024px / 768px 断点）
