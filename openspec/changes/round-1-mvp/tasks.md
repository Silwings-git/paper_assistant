## 0. 基础组件 (必须在业务代码之前完成)

- [x] 0.1 定义统一响应格式 ApiResponse<T> (code, message, data, timestamp)
- [x] 0.2 定义异常码枚举 ErrorCode (参数错误 / 资源不存在 / 外部API失败 / 限流 / LLM调用失败 / 内部错误)
- [x] 0.3 定义 BusinessException 自定义异常 (携带 ErrorCode 和详情)
- [x] 0.4 实现 GlobalExceptionHandler 全局异常处理 (ControllerAdvice, 返回统一 ApiResponse)
- [x] 0.5 配置 Spring CORS (允许 localhost:5173 前端开发服务器跨域请求)
- [x] 0.6 配置 logback-spring.xml (JSON 格式 / 按日志级别分离输出 / 文件滚动策略)
- [x] 0.7 实现 HTTP 请求日志拦截器 (记录 method, path, query, 耗时, 响应码, traceId)
- [x] 0.8 配置 MDC traceId (每次请求生成唯一 traceId, 日志中可追踪)
- [x] 0.9 配置 API 版本前缀 (所有 REST API 使用 /api/v1)
- [x] 0.10 配置 Bean Validation (spring-boot-starter-validation, 全局 MethodArgumentNotValidException 处理)
- [x] 0.11 创建 BaseEntity 基类 (id, createTime, updateTime, isDeleted 逻辑删除标识)
- [x] 0.12 配置 Jackson 全局序列化 (时区 Asia/Shanghai, 日期格式 yyyy-MM-dd HH:mm:ss, null 字段不输出)
- [x] 0.13 创建多环境配置 (application-dev.yml / application-prod.yml, spring.profiles.active=dev 默认)
- [x] 0.14 配置 HikariCP 连接池参数 (最大连接数, 空闲超时, 连接超时)
- [x] 0.15 添加 Spring Boot Test 依赖 + 第一个集成测试 (验证应用能正常启动)

## 1. 后端基础骨架

- [x] 1.1 使用 Spring Initializr 创建 Spring Boot 3.x 单模块项目 (Java 21)
- [x] 1.2 配置 pom.xml 依赖 (Spring Web, WebSocket, MyBatis-Plus, PostgreSQL, Redis, LangChain4j, Flyway, Validation, Test)
- [x] 1.3 配置 application.yml (数据源、HikariCP、Redis、异步线程池、WebSocket、DashScope API key、spring.profiles.active)
- [x] 1.4 配置 Spring Cache + Redis (RedisCacheManager + @EnableCaching)
- [x] 1.5 配置 Flyway (baseline-on-migrate, validate-on-migrate, 版本冲突自动回滚)
- [x] 1.6 创建 Flyway V1__init.sql 迁移脚本 (project, paper 含 UNIQUE(project_id,arxiv_id), analysis_result, llm_config, llm_model, task_status 表)
- [x] 1.7 编写 SQL 命名规范文档 (版本递增规则、不可修改已执行脚本、回滚策略)
- [x] 1.8 配置 MyBatis-Plus (PaginationInnerInterceptor 分页插件 + LogicalDeleteInnerInterceptor 逻辑删除 + 全局配置 + 开发环境 SQL 日志)
- [x] 1.9 实现 PostgreSQL JSONB TypeHandler (用于 authors JSONB、gaps/basePapers/innovationPts JSONB 字段映射)

## 2. 项目管理 (project-management)

- [x] 2.1 创建 Project 实体类 (继承 BaseEntity) 和 Mapper
- [x] 2.2 实现 ProjectService (create / update / getById / list / delete)，列表返回时包含 paperCount 字段 (通过 COUNT 查询关联论文数)
- [x] 2.3 实现 ProjectController (POST/PUT/GET/DELETE REST API), 入参使用 @Valid + DTO
- [x] 2.4 实现项目状态追踪 (CREATED → SEARCHING → SEARCHED → ANALYZING → ANALYZED，非法转换返回 ErrorCode)
- [x] 2.5 实现 base_paper_id 更新接口 (PUT /api/v1/projects/{id}/base-paper)
- [x] 2.6 实现项目逻辑删除 (MyBatis-Plus @TableLogic, 删除时级联标记 paper + analysis_result)

## 3. 论文检索 (paper-retrieval)

- [x] 3.1 实现 ArxivFetcher (调用 arXiv API, 解析 Atom XML, 提取 title/abstract/authors/publishDate/category/pdfUrl, 请求间隔 ≥3s, 单源上限 100 条)
- [x] 3.2 实现 SemanticScholarFetcher (调用 Semantic Scholar API, 补充 citationCount/influenceScore/hasCode/codeUrl, 5分钟≤100次, 单源上限 100 条)
- [x] 3.3 实现 PaperRetrievalService (双源聚合、Levenshtein 去重、排序 relevance/citation/date、分页、部分失败容错、单源上限 100 条)
- [x] 3.4 实现检索三层数据流: Redis缓存(@Cacheable TTL 24h) → DB 已保存结果 → 外部 API 新检索
- [x] 3.5 创建 Paper 实体类 (继承 BaseEntity) 和 Mapper (UNIQUE(project_id, arxiv_id) 幂等约束)
- [x] 3.6 实现检索结果幂等持久化 (ON CONFLICT 跳过, 关联 project_id)
- [x] 3.7 实现 PaperController:
  - POST /api/v1/papers/search?projectId=&keyword=  (触发异步检索, 返回 taskId)
  - GET /api/v1/papers?projectId=&page=&size=&sort=  (查询检索结果, sort=relevance|citation|date, 默认 relevance, size 默认 20 最大 100)
  - GET /api/v1/papers/{id}  (获取单篇论文详情, 用于论文详情抽屉)
  - GET /api/v1/papers/tasks/{taskId}  (查询任务状态)
  - 入参使用 DTO
- [x] 3.8 实现异步检索任务 (@Async + CompletableFuture + taskId 生成 + WebSocket STOMP 进度推送, 任务状态写入 task_status 表, 页面刷新后可恢复)
- [x] 3.9 创建 TaskStatus 实体类 (继承 BaseEntity) 和 Mapper (数据库持久化任务状态)
- [x] 3.10 实现任务状态持久化服务 (创建/更新/查询 task_status 记录, 替代内存 Map)

## 4. LLM 集成层 (llm-integration)

- [x] 4.1 定义 LlmProvider 接口 (chat / chatStructured / getProviderName / getSupportedModels)
- [x] 4.2 定义 LlmException 统一异常 (限流/超时/认证错误统一转换，认证错误不重试)
- [x] 4.3 实现 DashScopeProvider (基于 LangChain4j DashScope 集成)
- [x] 4.4 实现 LlmProviderFactory (根据 provider_type 动态实例化, 启动时从 DB 加载可用 provider, 无配置则启动失败)
- [x] 4.5 实现 LlmService (场景路由 analysis/writing/general + 默认模型回退 + 统一超时 120s)
- [x] 4.6 实现指数退避重试 (最多 3 次: 1s→2s→4s，各 provider 共享同一重试策略，认证错误不重试)
- [x] 4.7 创建 LlmConfig / LlmModel 实体和 Mapper (数据库配置管理)
- [x] 4.8 实现 LlmConfig 初始化数据 (Flyway V1 插入默认 DashScope 配置, provider_type=dashscope, model=qwen-plus)
- [x] 4.9 实现 PromptTemplates 常量类 (分析 prompt 模板)
- [x] 4.10 在 LlmProvider 接口添加 Javadoc，明确接口契约以便未来新增 provider 时参考
- [x] 4.11 实现分析场景 Prompt 模板管理 (PromptTemplates: 阶段1逐篇理解/阶段2交叉分析/阶段3创新生成, 三个独立模板)

## 5. 创新分析 (innovation-analysis)

- [x] 5.1 定义 PaperFullTextExtractor 接口 (责任链: extractFullText / getStrategyName / setNext) + PaperFullText 返回值 (fullText, strategy, textLength, warnings)
- [x] 5.2 定义 ExtractorStrategy 枚举 (latex, vision, ar5iv, pdfbox, abstract) + 枚举值对应的详细 Javadoc 说明 (优缺点、适用场景、成本)
- [x] 5.3 实现 PaperFullTextExtractorConfig 配置类 (从 application.yml 读取 extractor-order 列表, 按顺序组装责任链, 未配置时使用默认顺序)
- [x] 5.4 实现 LaTeXExtractor (下载 arXiv .tar.gz, 解压提取 .tex 文本, 保留公式源码, 有效性阈值 ≥500 字符, Javadoc 说明优缺点)
- [x] 5.5 实现 VisionExtractor (PDF→PNG 300DPI, 调用 qwen2.5-vl 视觉模型提取, 有效性阈值 ≥3 页, Javadoc 说明优缺点和 LLM 成本)
- [x] 5.6 实现 Ar5ivExtractor (访问 ar5iv HTML 版本, 提取文本, 公式转 MathJax, Javadoc 说明优缺点)
- [x] 5.7 实现 PdfBoxExtractor (PDFBox 纯文本提取, 处理公式乱码情况, Javadoc 说明优缺点)
- [x] 5.8 实现 AbstractExtractor (回退策略, 使用已有摘要填充 PaperFullText, Javadoc 说明优缺点)
- [x] 5.9 在 application.yml 中添加 paper.fulltext.extractor-order 默认配置
- [x] 5.10 定义 PaperAnalysis 实体 (逐篇理解结果: coreQuestion, methodologyCategory, keyContributions[], limitations[], keywords[], methodsAndDatasets)
- [x] 5.11 实现 PaperDeepAnalyzer (阶段 1: 逐篇深度理解 LLM 调用 + JSON Schema 验证 + 失败最多重试 2 次)
- [x] 5.12 实现 CrossAnalysisService (阶段 2: 交叉对比分析, 输入 N 篇 PaperAnalysis, 输出主题聚类/方法对比/冲突检测/共识点/缺口)
- [x] 5.13 实现 InnovationGenerator (阶段 3: 基于交叉分析结果生成研究空白/base 论文/创新点, 每个创新点必须关联交叉分析证据)
- [x] 5.14 定义分析结果 DTO (gaps[] 含分类+证据+支撑论文, basePapers[] 含推荐理由, innovationPts[] 含难度+贡献类型+证据)
- [x] 5.15 实现三阶段编排服务 AnalysisOrchestrator (阶段 1 并行 CompletableFuture.allOf → 阶段 2 → 阶段 3, 单篇失败跳过)
- [x] 5.16 实现 LangChain4j 结构化输出 (三阶段分别定义 JSON Schema, 验证失败最多重试 2 次)
- [x] 5.17 创建 AnalysisResult 实体类 (继承 BaseEntity) 和 Mapper
- [x] 5.18 实现分析结果持久化 (关联 project_id, JSONB 字段映射: gaps/basePapers/innovationPts)
- [x] 5.19 实现异步分析任务 (@Async + CompletableFuture + WebSocket STOMP 三阶段进度推送, 任务状态写入 task_status 表)
- [x] 5.20 实现 AnalysisController:
  - POST /api/v1/analysis/{projectId}  (触发异步分析, 返回 taskId)
  - GET /api/v1/analysis/{projectId}/result  (查询分析结果)
  - GET /api/v1/analysis/tasks/{taskId}  (查询任务状态)
- [x] 5.21 配置 LLM 分析专用线程池 (核心 5, 最大 10, 队列 20)

## 6. WebSocket 配置

- [x] 6.1 配置 Spring WebSocket + STOMP 子协议 (WebSocketConfig + MessageBrokerConfig, 心跳 30s)
- [x] 6.2 定义 STOMP 消息格式 (任务进度 / 错误通知, topic 命名: /topic/task/{taskId})
- [x] 6.3 实现 WebSocket 广播 (检索/分析任务完成后向 /topic/task/{taskId} 推送进度)

## 7. 定时任务包 (scheduler)

- [x] 7.1 创建 scheduler 包结构 (config/job/executor/controller 四个子包)
- [x] 7.2 配置 @EnableScheduling + SchedulerConfig (从 application.yml 读取 cron 表达式, 支持按 job 开关)
- [x] 7.3 定义 SchedulerJobExecutor 接口 (业务执行器抽象, execute() 返回执行结果: 清理条数/耗时/异常信息)
- [x] 7.4 实现 TaskStatusCleanupExecutor (注入 TaskStatusService, 执行清理逻辑, 返回执行结果)
- [x] 7.5 实现 TaskStatusCleanupJob (@Scheduled 定时调用 executor.execute(), 记录执行日志)
- [x] 7.6 实现 SchedulerJobController (HTTP API: POST /api/v1/scheduler/{job-name}/execute, 供未来 XXL-JOB 远程调用)

## 8. 前端基础 (frontend-mvp)

- [x] 8.1 使用 Vite 创建 Vue 3 + TypeScript 项目
- [x] 8.2 安装依赖 (Vue 3, Vue Router, Pinia, axios, Element Plus, @stomp/stompjs, TypeScript)
- [x] 8.3 配置 Vite (代理 /api 和 /ws 到后端 8080, 路径别名 @)
- [x] 8.4 定义 TypeScript 类型系统 (types/api.ts: ApiResponse/ErrorCode, types/project.ts: ProjectDTO/ProjectStatus, types/paper.ts: PaperDTO/SortOrder, types/analysis.ts: AnalysisResultDTO/ResearchGap/BasePaperRecommendation/InnovationPoint)
- [x] 8.5 封装 axios 请求实例 (baseURL, 超时, 统一响应拦截: 解析 ApiResponse.code + Element Plus Message 提示 + 网络异常降级)
- [x] 8.6 配置 Vue Router (Dashboard → /, ProjectDetail → /projects/:id, AnalysisReport → /projects/:id/analysis)
- [x] 8.7 配置 Pinia + 三个 store (project: fetchList/create/update/remove/setCurrent, paper: search/fetchList/selectPaper/clearSelection/fetchTaskStatus, analysis: trigger/fetchResult/fetchTaskStatus)
- [x] 8.8 实现 WebSocket 连接管理 StompManager (自动连接、STOMP 心跳 30s、订阅/取消、自动重连 5 次后降级为 HTTP 轮询)
- [x] 8.9 实现通用组件 AppHeader (全局顶部导航栏)
- [x] 8.10 实现通用组件 BreadcrumbNav (面包屑导航, props: items 数组, 最后一项不可点击)
- [x] 8.11 实现通用组件 EmptyState (通用空状态, props: title/description/actionText/onAction)
- [x] 8.12 实现通用组件 StateTag (项目状态标签, props: ProjectStatus → 中文标签 + Element Plus 颜色)
- [x] 8.13 实现工具函数 format.ts (日期格式化、数字格式化 1234→"1.2k"、超长文本截断)
- [x] 8.14 实现工具函数 status.ts (状态映射 + 操作禁用判断: getProjectStatusInfo)
- [x] 8.15 提取 mockup 设计系统变量 (颜色/圆角/阴影/字体 → CSS 变量), 建立全局样式文件, 严格复用 html-mockups/ 目录中的高保真设计

## 9. 前端页面实现

- [x] 9.1 实现 Dashboard 页面 (严格复用 html-mockups/01-dashboard.html: hero 区 + 项目卡片网格 + 新建对话框 + 删除确认 + 状态标签 + 操作按钮按状态变化)
- [x] 9.2 实现 ProjectDetail 页面 (严格复用 html-mockups/02-project-detail.html: 面包屑 + 项目信息区 + Base 论文卡片 + 搜索区 + 警告横幅 + 论文表格 + 分页 + 操作区 + 任务进度面板)
- [x] 9.3 实现 ProjectDetail 的 Base 论文卡片 (已选 Base 摘要展示 + 更换按钮 / 未选时显示"选择 Base 论文 →")
- [x] 9.4 实现 BasePaperSelector 组件 (严格复用 02-project-detail.html 中的 selector 面板: 620px modal + 自定义 radio + 标题/作者/meta + 取消/确认)
- [x] 9.5 实现论文详情抽屉 PaperDetailDrawer (严格复用 02-project-detail.html: 右侧滑出 540px + sticky 头部 + 标题 → meta → 摘要 → PDF/代码链接)
- [x] 9.6 实现 AnalysisReport 页面 (严格复用 html-mockups/03-analysis-report.html: 报告头部 + 研究空白 2 列网格 + Base 推荐列表 + 创新点 3 列网格 + 面包屑)
- [x] 9.7 实现全局加载状态 (按钮级 loading + 状态驱动禁用: 基于 getProjectStatusInfo.disabledActions)
- [x] 9.8 实现全局错误状态 (Element Plus Message 错误提示 + 重试按钮 + 部分失败黄色警告条)

## 10. 联调与测试

- [ ] 10.1 本地启动前后端，验证 API 连通性
- [ ] 10.2 端到端测试: 创建项目 → 搜索论文 → 查看结果 → 触发分析 → 查看报告
- [ ] 10.3 验证 WebSocket 实时进度推送 + 断线降级轮询
- [ ] 10.4 验证 arXiv/Semantic Scholar 部分失败 UI 提示 (黄色警告条)
- [ ] 10.5 验证 LLM 分析三阶段进度展示 (TaskProgressPanel 阶段指示器)
