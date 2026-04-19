## ADDED Requirements

### Requirement: 分析结果追加不覆盖

每次执行分析任务时，系统 MUST 创建新的 `analysis_result` 记录，不得覆盖或删除已有的分析结果。

#### Scenario: 第二次分析保留第一次结果
- **WHEN** 用户对项目 A 执行第二次分析任务并成功完成
- **THEN** `analysis_result` 表中存在两条该项目 A 的分析记录

#### Scenario: 查询返回最新分析结果
- **WHEN** 调用 `GET /api/v1/projects/{id}/analysis`
- **THEN** 默认返回 `create_time` 最新的一条分析结果

### Requirement: 查询历史分析结果

系统 MUST 提供接口查询项目的所有历史分析结果，按时间倒序排列。

#### Scenario: 获取分析历史列表
- **WHEN** 调用 `GET /api/v1/projects/{id}/analysis/history`
- **THEN** 返回该项目所有分析结果，按 `create_time` 降序排列，每条包含 `id`、`create_time`、分析论文数量摘要

#### Scenario: 获取指定版本分析结果
- **WHEN** 调用 `GET /api/v1/projects/{id}/analysis/{resultId}`
- **THEN** 返回指定 ID 的分析结果详情

### Requirement: 删除过时分析结果

用户 MUST 能够删除不需要的分析结果记录，不影响论文数据和其他分析结果。

#### Scenario: 删除旧版本分析结果
- **WHEN** 调用 `DELETE /api/v1/projects/{id}/analysis/{resultId}`
- **THEN** 仅删除指定 `analysis_result` 记录，project 的论文数据和其余分析结果不受影响
