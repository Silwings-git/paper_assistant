# 设计决策

## 1. DashScope 集成方式

- **使用 LangChain4j Community DashScope 模块** (`langchain4j-community-dashscope`)
- `DashScopeProvider` 实际调用 `LlmService.chat()` 中已有的 `chat()` 方法
- 结构化输出使用 LangChain4j 的 `@StructuredOutput` 或手动 JSON 解析 + 重试

## 2. 三阶段分析实现

- 阶段1：调用 `qwen-plus` 逐篇分析，解析 JSON 为 `PaperAnalysisDTO`
- 阶段2：收集所有 PaperAnalysis 后调用 `qwen-plus` 交叉分析，解析为 `CrossAnalysisDTO`
- 阶段3：基于交叉分析结果调用 `qwen-plus` 生成创新点，解析为 `InnovationAnalysisDTO`
- 每阶段 JSON 解析失败最多重试 2 次

## 3. Redis 缓存策略

- 使用 `@Cacheable(value = "papers", key = "#projectId + ':' + #keyword", cacheManager = "redisCacheManager")`
- TTL 24 小时（已在 RedisCacheConfig 中配置）
- 缓存 key 格式：`retrieval:{projectId}:{keyword}`

## 4. TaskProgressPanel 组件

- 嵌入在 ProjectDetail 页面，非独立路由
- 使用 Element Plus `el-progress` + 状态标签
- 通过 WebSocket 或 HTTP 轮询获取任务状态

## 5. WebSocket 降级

- StompManager 维护重连计数器
- 超过 5 次重连失败后，切换到 HTTP 轮询（每 3s 查询一次任务状态）

## 6. Semantic Scholar 限流

- 使用简单的滑动窗口计数器（内存中）
- 5 分钟内最多 100 次请求
- 达到上限时等待或返回部分结果
