## Context

前端页面目前使用 Element Plus 组件（el-card、el-dialog、el-table 等）渲染，视觉风格与设计稿（html-mockups/*.html）完全不同。设计稿使用自定义 CSS 变量系统，定义了精确的颜色、圆角、阴影、间距和组件样式。

现有数据层情况：
- `stores/project.ts` → `getProjects()`, `createProject()`, `deleteProject()`, `setProjectBasePaper()`
- `stores/paper.ts` → `searchPapersApi()`, `getPapersApi()`, `getPaperTaskStatusApi()`
- `stores/analysis.ts` → `triggerAnalysisApi()`, `getAnalysisResultApi()`, `getAnalysisTaskStatusApi()`
- `types/*.ts` → 类型转换函数（toProjectDTO, toPaperDTO, toAnalysisResultDTO）
- `api/index.ts` → Axios 拦截器统一解包 `{ code: 200, data: T }`

## Goals / Non-Goals

**Goals:**
- 三个页面（Dashboard、ProjectDetail、AnalysisReport）的视觉效果精确匹配设计稿
- 每个页面完全重新对接后端接口，确保 UI 变更不破坏数据流
- 组件交互功能（弹窗、抽屉、分页、排序）完整可用

**Non-Goals:**
- 不改变后端 API 或数据结构
- 不引入新的 CSS 框架（Tailwind 等）
- 不修改路由或页面结构
- 不完全移除 Element Plus（仍保留 ElMessage 等工具类组件）

## Decisions

**1. 直接在 .vue 文件中使用原生 HTML + CSS，不新建通用组件库**

将设计稿的 HTML 结构和 CSS 直接嵌入对应的 Vue 组件中。不抽象全局通用组件，因为：
- 当前只有三个页面，抽象成本高于复用收益
- 设计稿样式紧密耦合到页面结构，强行拆分会引入额外适配层
- 保留 Element Plus 作为渐进过渡工具

**2. 交互状态用 Vue `ref` + `class` 绑定替代原生 JS**

设计稿中的 `classList.add/remove('hidden')` 改为 Vue 响应式状态：
```vue
// 设计稿
document.getElementById('createModal').classList.remove('hidden')

// Vue
const createModalVisible = ref(false)
<button @click="createModalVisible = true">
<div :class="{ hidden: !createModalVisible }">
```

**3. CSS 变量通过全局 :root 或 scoped :root 注入**

设计稿的 `:root { --bg-primary: ... }` 变量系统保持不变，放入 App.vue 的全局样式中避免重复。

**4. 表格用原生 `<table>` 替代 `el-table`**

el-table 渲染出复杂的内部 DOM 结构，无法通过 CSS 覆盖匹配设计稿。改用原生 `<table>` + Vue `v-for` 渲染 `<tr>`，结构与设计稿完全一致。

**5. 数据对接：保留现有 stores + API 层，每页面重写前先行验证**

不修改 stores 或 API 层的实现。每个页面重写时：
1. 先验证该页面所调用的 API 函数返回的数据格式（字段名、null/undefined 处理）
2. 验证类型转换函数（toXXX）输出与设计稿模板所需字段匹配
3. 基于验证结果编写模板，确保每个 `v-bind` 绑定的字段名正确
4. 完成后走通完整数据流（创建→搜索→分析→报告）

**6. 异步操作（轮询/进度条/状态恢复）是重点，必须完整保留**

当前存在三类异步操作，重写时必须一一对接：

**6a. 任务轮询机制**

Stores 使用 `setInterval` 每 2 秒轮询任务状态，新 UI 必须保留：
```
paperStore.search()     → setInterval → getPaperTaskStatusApi()
analysisStore.trigger() → setInterval → getAnalysisTaskStatusApi()
```
- 页面 `onMounted` 时，如果有进行中的任务，恢复轮询
- 页面 `onUnmounted` 时，调用 `stopSearchPolling()` / `stopAnalyzePolling()` 清除 timer
- 轮询终止条件：status 为 `SEARCHED` / `ANALYZED` / `FAILED` / `SEARCHED_WITH_ERRORS`

**6b. 进度条映射**

设计稿中的进度条需要映射 store 中的 `TaskStatusDTO` 字段：
```
design:  <div class="progress-bar-fill" style="width: 78%;">
         <span class="progress-stage">正在从 Semantic Scholar 获取数据...</span>
         <span class="progress-percent">78%</span>

store:   taskStatus.progress   → width
         taskStatus.message    → stage text
         taskStatus.progress   → percent + '%'
```
- 检索进度：蓝色进度条，绑定 `paperStore.currentTaskStatus`
- 分析进度：紫色进度条，绑定 `analysisStore.currentTaskStatus`
- 两个进度条可同时出现在 ProjectDetail 页面

**6c. 页面恢复逻辑**

页面刷新后需要恢复任务状态：
```
onMounted → 检查 paperStore.currentTaskId
          → 如果有，调用 fetchTaskStatus() 获取最新状态
          → 如果 status === 'SEARCHING' / 'ANALYZING'，重新触发轮询
```
ProjectDetail 和 AnalysisReport 都有此逻辑。

**6d. 分析页面额外进度轮询**

AnalysisReport 页面有独立的 `watch` 监听 `analysisStore.currentTaskStatus`，
用于动态更新按钮 loading 状态和进度条样式，新 UI 必须保留此 watch。

## Risks / Trade-offs

- **字段名不一致**：设计稿中的字段名可能与后端 DTO 字段不同（如 `authorNames` vs `authors`）→ 每个页面先验证字段映射
- **Element Plus 依赖未完全移除**：部分页面仍引用了 ElMessage 等，混合使用可能导致样式冲突 → 用 `:deep()` 隔离或保留必要引用
- **响应式断点**：设计稿包含 `@media` 查询，需在 Vue scoped 样式中保留
- **组件复用缺失**：按钮、徽章等未抽象为独立组件，未来新增页面需复制样式 → 可在后续轮次抽取
- **长文本溢出**：表格中论文标题在 el-table 有自动截断，原生 table 需手动加 `text-overflow`
- **空数据/加载态**：设计稿未覆盖所有空状态 → 保留 `v-if` 判断，数据为空时显示简化提示
