## Why

当前前端页面使用 Element Plus 组件默认样式，与 html-mockups/ 目录下的高保真设计稿完全不一致。设计稿定义了精确的配色系统、圆角、阴影、间距和组件交互效果，但当前实现使用的是 Element Plus 的视觉风格，导致页面外观与设计意图差异很大。

## What Changes

- 将 Dashboard、ProjectDetail、AnalysisReport 三个页面的 UI 从 Element Plus 组件替换为高保真设计稿的自定义组件
- 移除对 el-card、el-dialog、el-table、el-pagination、el-progress、el-empty、el-alert 的依赖
- 引入设计稿中的完整 CSS 变量系统（颜色、圆角、阴影、间距）
- 保留现有数据绑定、状态管理、API 调用逻辑不变

## Capabilities

### New Capabilities

- `ui-components`: 自定义 UI 组件系统（按钮、徽章、卡片、弹窗、抽屉、表格、进度条、分页、空状态），完全匹配设计稿样式
- `dashboard-page`: 项目仪表盘页面，精确还原 01-dashboard.html 的视觉设计
- `project-detail-page`: 项目详情页，精确还原 02-project-detail.html 的视觉设计
- `analysis-report-page`: 分析报告页，精确还原 03-analysis-report.html 的视觉设计

### Modified Capabilities

<!-- 无现有 spec 需要修改 -->

## Impact

- **前端文件**: `Dashboard.vue`、`ProjectDetail.vue`、`AnalysisReport.vue` 将大幅重写模板和样式部分
- **组件文件**: 新增或替换多个通用组件（按钮、卡片、弹窗等）
- **保留不变**: stores/（数据层）、API 调用、路由配置、类型定义
- **依赖影响**: 移除部分 Element Plus 组件引用，保留 `ElMessage` 等通知类组件
