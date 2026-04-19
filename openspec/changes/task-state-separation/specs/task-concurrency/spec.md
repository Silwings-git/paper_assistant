## ADDED Requirements

### Requirement: 按任务类型互斥

系统 MUST 保证同一项目同一任务类型（SEARCH / ANALYSIS）最多只有一个 RUNNING 状态的任务。当用户尝试启动新任务且存在同类型活跃任务时，系统 MUST 拒绝并返回错误提示。

#### Scenario: 同类型任务已存在活跃任务
- **WHEN** 用户在项目 A 已有 SEARCH 任务处于 RUNNING 状态时尝试再次搜索
- **THEN** 系统返回错误，提示"当前有检索任务正在执行，请先取消或等待完成"

#### Scenario: 不同类型任务可并行
- **WHEN** 用户在项目 A 有 SEARCH 任务处于 RUNNING 状态时尝试启动分析
- **THEN** 系统允许创建 ANALYSIS 任务并正常执行

#### Scenario: 任务完成后允许新任务
- **WHEN** 用户在项目 A 的 SEARCH 任务状态为 COMPLETED / FAILED / CANCELLED 后再次搜索
- **THEN** 系统允许创建新的 SEARCH 任务

### Requirement: 异常 RUNNING 任务超时检测

系统 MUST 对超过 10 分钟仍处于 RUNNING 状态的任务进行检测，并允许新任务启动以替代异常任务。

#### Scenario: 超时任务存在时允许替代
- **WHEN** 用户在项目 A 的 SEARCH 任务已 RUNNING 超过 10 分钟时尝试再次搜索
- **THEN** 系统允许创建新的 SEARCH 任务，旧任务标记为异常

### Requirement: 并发检查在服务层执行

并发控制 MUST 在 Service 层（`TaskConcurrencyService`）执行，而非 Controller 层，以保证所有任务入口点都被覆盖。

#### Scenario: 通过 Service 创建任务时自动检查
- **WHEN** 任何 Service 方法调用 `TaskConcurrencyService.hasActiveTask()` 并返回 true
- **THEN** 调用方 MUST 抛出 `BusinessException` 并中止任务创建
