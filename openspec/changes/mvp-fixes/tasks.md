## Phase 1: DashScope 集成（CRITICAL）

- [x] 1.1 实现 DashScopeProvider.chat() — 调用 LangChain4j ChatModel，返回 LLM 响应文本
- [x] 1.2 实现 DashScopeProvider.chatStructured() — 调用 LangChain4j 结构化输出，返回 JSON 字符串
- [x] 1.3 实现 DashScopeProvider.getProviderName() — 返回 "dashscope"
- [x] 1.4 实现 DashScopeProvider.getSupportedModels() — 返回支持的模型列表
- [x] 1.5 添加 LangChain4j ChatModel Bean 配置（通过 LlmProviderFactory 或 Spring 自动配置）

## Phase 2: 三阶段分析实现（CRITICAL）

- [x] 2.1 实现 PaperDeepAnalyzer.analyze() — 调用 LLM 解析 JSON 为 PaperAnalysisDTO，失败重试 2 次
- [x] 2.2 实现 CrossAnalysisService.analyze() — 收集 N 篇分析结果调用 LLM，输出 CrossAnalysisDTO
- [x] 2.3 实现 InnovationGenerator.generate() — 基于交叉分析结果调用 LLM，输出 InnovationAnalysisDTO
- [x] 2.4 实现 AnalysisOrchestrator 三阶段数据传递 — 阶段1输出→阶段2输入→阶段3输入
- [x] 2.5 添加分析数量上限检查（最多 20 篇，超出跳过）
- [x] 2.6 修复进度百分比（阶段1: 0-85%, 阶段2: 85-90%, 阶段3: 90-100%）

## Phase 3: 检索三层数据流（CRITICAL）

- [x] 3.1 在 PaperRetrievalService.search() 添加 @Cacheable 注解（key = projectId:keyword, TTL 24h）
- [x] 3.2 实现 DB 回退逻辑 — 缓存未命中时查询 DB 中已有的检索结果
- [x] 3.3 实现外部 API 调用 — DB 无结果时调用外部 API 并写入缓存和 DB

## Phase 4: 前端组件补全（WARNING）

- [x] 4.1 创建 TaskProgressPanel.vue 组件 — 显示阶段指示器、进度条、状态消息
- [x] 4.2 在 ProjectDetail.vue 中集成 TaskProgressPanel — 替换现有的 el-progress 内联展示
- [x] 4.3 实现 StompManager 降级逻辑 — 重连 5 次失败后切换到 HTTP 轮询

## Phase 5: 限流与健壮性（WARNING）

- [x] 5.1 实现 SemanticScholarFetcher 滑动窗口限流 — 5 分钟 100 次请求
- [x] 5.2 实现 LlmProviderFactory 从 DB 加载 provider（可选，MVP 可延后）
- [x] 5.3 添加 LLM 场景路由逻辑（可选，MVP 可延后）
