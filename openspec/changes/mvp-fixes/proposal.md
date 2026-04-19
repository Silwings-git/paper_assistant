# MVP 验证修复

## 背景

对 round-1-mvp 实施完成后的 OpenSpec 验证发现以下问题需要修复：

## CRITICAL 问题

1. **DashScopeProvider 是空壳** — `chat()` 和 `chatStructured()` 抛出 `UnsupportedOperationException`，所有 LLM 功能不可用
2. **三阶段分析全是 TODO 桩** — `PaperDeepAnalyzer`、`CrossAnalysisService`、`InnovationGenerator` 均返回空数据
3. **AnalysisOrchestrator 保存空结果** — 直接写入 `"[]"` 作为分析结果
4. **检索三层数据流未实现** — `@Cacheable` 导入了但从未使用，不查 Redis 也不查 DB
5. **5 个测试任务未完成** — Section 10 全部未勾选

## WARNING 问题

6. Semantic Scholar 限流未实施（100次/5分钟）
7. LLM 场景路由未生效
8. LlmProviderFactory 不从 DB 加载 provider
9. TaskProgressPanel.vue 组件缺失
10. WebSocket 降级轮询未实现
11. 分析数量上限缺失（20 篇）
12. 进度百分比与 Spec 不一致
13. 启动时缺少 DashScope key 仅 warn（应启动失败）
