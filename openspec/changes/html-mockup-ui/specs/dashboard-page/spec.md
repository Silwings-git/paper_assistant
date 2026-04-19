## ADDED Requirements

### Requirement: 项目仪表盘页面布局
页面 SHALL 包含顶部 Header（Logo + 导航）、Hero 区域（标题+描述）、项目网格三个层级。

#### Scenario: 渲染完整仪表盘
- **WHEN** 用户访问首页
- **THEN** 显示 56px 高度 sticky header、28px 标题 Hero 区域、响应式项目网格（最小 320px 每列）

### Requirement: 项目创建弹窗
页面 SHALL 提供新建项目弹窗，包含名称（必填）、研究方向（必填）、描述（可选）三个字段。

#### Scenario: 打开创建项目弹窗
- **WHEN** 用户点击「新建项目」按钮
- **THEN** 弹出 460px 宽的模态面板，含三个表单字段和取消/创建按钮

### Requirement: 项目删除确认弹窗
页面 SHALL 提供删除确认弹窗，带警告图标和确认按钮。

#### Scenario: 点击删除按钮
- **WHEN** 用户点击项目卡片的「删除」按钮
- **THEN** 弹出居中对话框，显示红色警告图标、「取消」和「确认删除」按钮

### Requirement: 项目卡片交互
项目卡片 SHALL 支持 hover 阴影加深，点击卡片导航到项目详情页。

#### Scenario: 点击项目卡片
- **WHEN** 用户点击项目卡片
- **THEN** 路由导航到 /projects/{id}
