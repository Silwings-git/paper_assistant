## ADDED Requirements

### Requirement: 按钮组件
系统 SHALL 提供与设计稿一致的按钮样式，包括 primary、ghost、outline、danger、sm 变体。

#### Scenario: 渲染 primary 按钮
- **WHEN** 页面渲染一个 `.btn.btn-primary` 元素
- **THEN** 显示蓝色背景（--accent: #2563eb）、白色文字、10px 圆角，hover 时背景色变为 --accent-hover

#### Scenario: 渲染 sm 尺寸按钮
- **WHEN** 按钮同时添加 `.btn-sm` 类
- **THEN** padding 为 5px 12px，字体 13px，圆角 8px

### Requirement: 徽章组件
系统 SHALL 提供状态徽章，支持 created、searching、searched、analyzing、analyzed 五种状态，每种状态有独立配色和圆点动画。

#### Scenario: 渲染 searching 状态徽章
- **WHEN** 页面渲染 `.badge.badge-searching` 元素
- **THEN** 显示黄色背景（--warning-light），内含 6px 圆点并带 pulse 动画（1.5s 循环，opacity 1→0.4）

#### Scenario: 渲染 analyzed 状态徽章
- **WHEN** 页面渲染 `.badge.badge-analyzed` 元素
- **THEN** 显示紫色背景（--purple-light），圆点为紫色，无动画

### Requirement: 卡片组件
系统 SHALL 提供项目卡片、研究空白卡片、推荐论文卡片、创新点卡片等，均使用统一圆角（12px）和阴影系统。

#### Scenario: 渲染项目卡片
- **WHEN** 页面渲染 `.project-card` 元素
- **THEN** 显示白色背景、1px 边框、12px 圆角、基础阴影，hover 时阴影加深且 translateY(-1px)

### Requirement: 弹窗组件
系统 SHALL 提供遮罩+面板结构的弹窗，包括创建项目弹窗和确认删除弹窗。

#### Scenario: 渲染创建项目弹窗
- **WHEN** `createModalVisible` 为 true
- **THEN** 显示半透明遮罩（rgba 黑色 35%），居中显示 460px 白色面板，面板圆角 12px

### Requirement: 侧边抽屉组件
系统 SHALL 提供右侧滑出的抽屉面板，用于展示论文详情。

#### Scenario: 渲染论文详情抽屉
- **WHEN** `drawerVisible` 为 true
- **THEN** 显示遮罩，右侧滑出 540px 宽面板，带顶部 sticky header 和关闭按钮

### Requirement: 原生表格组件
系统 SHALL 使用原生 `<table>` 元素替代 el-table，表头带灰色背景、工具栏带排序标签页。

#### Scenario: 渲染论文列表表格
- **WHEN** 页面渲染论文列表
- **THEN** 使用原生 table，thead 背景色 --bg-table-head，tbody tr hover 变色，选中行背景色 --accent-light

### Requirement: 分页组件
系统 SHALL 提供分页控件，包含页码按钮、前后翻页、总数信息。

#### Scenario: 渲染分页控件
- **WHEN** 渲染 `.pagination` 元素
- **THEN** 左侧显示总数文字，右侧显示页码按钮，当前页按钮蓝色背景白色文字，禁用按钮 opacity 0.3

### Requirement: 进度条组件
系统 SHALL 提供任务进度条，包含 6px 细轨道、百分比数字、阶段描述。

#### Scenario: 渲染检索进度
- **WHEN** 渲染检索任务进度面板
- **THEN** 显示 6px 高轨道，蓝色填充条宽度随进度变化，底部显示阶段描述和百分比

### Requirement: 空状态组件
系统 SHALL 提供空状态提示，用于无项目、无结果场景。

#### Scenario: 显示无项目空状态
- **WHEN** 项目列表为空
- **THEN** 显示居中提示文字，不含冗余边框

### Requirement: CSS 变量系统
系统 SHALL 使用设计稿定义的 CSS 变量（--bg-primary, --accent, --radius-md 等）作为全局设计 token。

#### Scenario: 加载全局样式
- **WHEN** 应用启动
- **THEN** :root 中包含全部颜色、圆角、阴影变量，所有组件通过 var() 引用
