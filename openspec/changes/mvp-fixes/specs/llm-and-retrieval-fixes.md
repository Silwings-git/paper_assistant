# Delta Spec: LLM 集成修复

## Requirement: DashScope 实际集成

后端必须能够调用 DashScope API 进行对话，而非抛出 UnsupportedOperationException。

### Scenario: 普通对话
- **Given** DashScope API key 已配置
- **When** 调用 `LlmService.chat()` 
- **Then** 返回 LLM 的实际响应文本

### Scenario: API key 未配置
- **Given** 环境变量中无 DASHSCOPE_API_KEY
- **When** 应用启动
- **Then** 打印 warn 日志并继续启动（MVP 阶段允许无 key 运行）

---

# Delta Spec: 三阶段分析实现

后端必须实际调用 LLM 完成三阶段分析，而非返回空数据。

### Scenario: 完整分析流程
- **Given** 项目中有论文数据
- **When** 触发分析任务
- **Then** 阶段1：逐篇深度理解，提取 coreQuestion/methodology/keyContributions
- **Then** 阶段2：交叉对比分析，生成主题聚类和缺口
- **Then** 阶段3：生成研究空白/base 推荐/创新点

### Scenario: 分析数量上限
- **Given** 项目中超过 20 篇论文
- **When** 触发分析任务
- **Then** 只分析前 20 篇，超出部分跳过

---

# Delta Spec: 检索三层数据流

论文检索必须遵循 Redis 缓存 → DB → 外部 API 的三层数据流。

### Scenario: 缓存命中
- **Given** 相同的搜索关键词已在 24h 内检索过
- **When** 再次发起相同搜索
- **Then** 直接从 Redis 缓存返回结果，不调用外部 API

### Scenario: DB 已有结果
- **Given** 缓存未命中但该项目已保存过检索结果
- **When** 发起搜索
- **Then** 从 DB 返回已保存的结果

### Scenario: 新检索
- **Given** 缓存和 DB 均无结果
- **When** 发起搜索
- **Then** 调用外部 API，结果写入 DB 和 Redis 缓存

---

# Delta Spec: 前端组件补全

前端必须实现任务进度面板组件。

### Scenario: 任务进度展示
- **Given** 检索或分析任务正在执行
- **When** 用户停留在项目详情页
- **Then** TaskProgressPanel 显示当前阶段、进度百分比和状态消息

### Scenario: WebSocket 降级
- **Given** WebSocket 连接断开且重连超过 5 次
- **When** 需要获取任务状态
- **Then** 自动降级为 HTTP 轮询

---

# Delta Spec: 限流与健壮性

外部 API 调用必须有合理的限流保护。

### Scenario: Semantic Scholar 限流
- **Given** 5 分钟内已发送 100 次请求
- **When** 再次尝试调用 Semantic Scholar API
- **Then** 等待至窗口期结束或返回友好错误
