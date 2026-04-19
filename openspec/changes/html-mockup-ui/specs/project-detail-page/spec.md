## ADDED Requirements

### Requirement: 项目详情页布局
页面 SHALL 包含顶部 Header、面包屑导航、项目信息头部、Base 论文卡片、搜索区域、论文表格、操作区域、任务进度面板。

#### Scenario: 渲染项目详情页
- **WHEN** 用户访问 /projects/{id}
- **THEN** 显示面包屑导航、项目标题+徽章、Base 论文卡片（如有）、论文表格、触发分析按钮

### Requirement: Base 论文卡片
页面 SHALL 展示已选定的 Base 论文信息，含论文标题、作者、更换按钮。

#### Scenario: 显示 Base 论文
- **WHEN** 项目已选定 Base 论文
- **THEN** 显示卡片含论文标题、作者行，右侧有「更换」outline 按钮

#### Scenario: 未选择 Base 论文
- **WHEN** 项目未选定 Base 论文
- **THEN** 显示提示文字「尚未选择 Base 论文，请选择」和「选择论文」按钮

### Requirement: 论文选择器弹窗
页面 SHALL 提供论文选择弹窗，含单选按钮、论文列表、确认选择按钮。

#### Scenario: 更换 Base 论文
- **WHEN** 用户点击「更换」按钮
- **THEN** 弹出 620px 宽选择面板，每篇论文前有圆形单选按钮，底部有取消和确认按钮

### Requirement: 论文详情抽屉
页面 SHALL 提供论文详情抽屉，含标题、元信息、摘要、链接按钮。

#### Scenario: 查看论文详情
- **WHEN** 用户点击表格中的论文行
- **THEN** 右侧滑出 540px 宽抽屉，显示论文完整标题、作者、日期、摘要、PDF/代码链接

### Requirement: 警告横幅
页面 SHALL 在数据源部分不可用时显示黄色警告横幅。

#### Scenario: 显示警告横幅
- **WHEN** Semantic Scholar 数据源不可用
- **THEN** 在搜索框下方显示黄色背景警告条，含警告图标和提示文字
