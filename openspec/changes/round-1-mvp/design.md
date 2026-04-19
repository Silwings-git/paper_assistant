## Context

Paper Assistant 是一个从零开始的新项目。当前没有任何源代码。Round 1 MVP 的目标是建立基础架构并跑通"论文检索 → LLM 分析 → 选择 base 论文"的核心链路，为后续的代码复现、实验设计、论文撰写等模块奠定基础。

**技术栈**: Spring Boot (Java) + Vue 3 (TypeScript) + PostgreSQL + Redis + DashScope (LLM)

**约束**:
- 用户擅长 Java + Rust，但 Round 1 暂不涉及 Rust 执行引擎
- LLM 使用阿里百炼 (DashScope)，需预留多 provider 扩展能力
- 前端使用 Vue 3 + Element Plus

## Goals / Non-Goals

**Goals:**
- 建立 Spring Boot + Vue + PostgreSQL 的基础项目骨架
- 实现 arXiv + Semantic Scholar 双源论文检索与结果聚合
- 通过 LLM 批量分析论文，输出研究空白和 base 论文推荐
- 实现项目管理 CRUD 和状态追踪
- 实现 LLM provider 抽象层，DashScope 为首个接入提供者
- 提供最小可用前端界面

**Non-Goals:**
- Rust 执行引擎和 Docker 容器化（后续 Round）
- 代码复现和实验执行（后续 Round）
- 论文撰写和导出（后续 Round）
- 多用户权限系统（Round 1 为单用户/硬编码）
- LLM 多 provider 实际切换（仅预留接口，Round 1 只用 DashScope）

## Decisions

### D1: Spring Boot 3.x + Java 21

选择 Spring Boot 3.x，搭配 Java 21。利用虚拟线程简化异步任务管理（论文检索和 LLM 分析都是长耗时操作）。Spring 生态对 LLM 集成有两个选项：Spring AI 和 LangChain4j。

**选择 LangChain4j**：它对国内 LLM provider 支持更成熟，DashScope 集成更完善，且结构化输出（JSON schema）支持比 Spring AI 更稳定。

**替代方案**: Spring AI — 官方维护但社区生态较新，DashScope 支持有限。

### D2: 前端使用 Vue 3 + Vite + Element Plus

Vue 3 组合式 API + Vite 构建，UI 框架选择 Element Plus（组件丰富、文档完善、与 Vue 3 兼容性好）。前端使用 axios 调用后端 REST API。

**替代方案**: React + Ant Design — 用户无明确偏好，Vue 开发效率更高。

### D3: REST API + WebSocket

论文检索和 LLM 分析是异步长任务，采用双重通信模式：
- REST API 处理 CRUD 操作（项目、论文查询等）
- WebSocket 推送异步任务的进度更新（检索进度、分析进度）

**替代方案**: SSE (Server-Sent Events) — 单向推送足够，但 WebSocket 为后续 Rust daemon 双向通信预留能力。

### D4: PostgreSQL + MyBatis-Plus

PostgreSQL 作为关系型数据库，ORM 选择 MyBatis-Plus。相比 JPA/Hibernate，MyBatis-Plus 更轻量，SQL 可控，且对复杂查询（论文检索结果的过滤、排序、分页）支持更好。

**替代方案**: JPA/Hibernate — 更重量级但 Round 1 的 CRUD 不需要其复杂特性。

### D4.1: Redis 缓存

使用 Redis 缓存 arXiv/Semantic Scholar 检索结果（TTL 24 小时），避免重复请求触发外部 API 限流。

**三层数据流**:
```
请求关键词搜索 (projectId + keyword)
  → 1. 查 Redis 缓存 (key: "search:{projectId}:{keyword}")
       → 命中: 直接返回
  → 2. 查 DB (paper 表中 project_id 关联的记录)
       → 已有: 返回 DB 结果 + 写回 Redis
  → 3. 调用外部 API (arXiv + Semantic Scholar)
       → 新检索: 写 DB + 写 Redis
```

使用 Spring Cache 集成，通过 `@Cacheable` 注解声明式缓存，业务代码无侵入。

**替代方案**: 用 PostgreSQL 表做缓存 — 需要手动管理过期清理，且查询性能不如 Redis。

### D5: LLM 抽象层设计

定义 `LlmProvider` 接口，包含以下核心方法：

```java
public interface LlmProvider {
    // 普通文本对话
    String chat(String systemPrompt, String userPrompt);
    
    // 结构化输出 (JSON → Java Object)
    <T> T chatStructured(String systemPrompt, String userPrompt, Class<T> responseSchema);
    
    // 提供者标识 (用于日志和路由)
    String getProviderName();
    
    // 支持的模型列表
    List<String> getSupportedModels();
}
```

**实现规划**：
- Round 1: `DashScopeProvider` (基于 LangChain4j 的 DashScope 集成)
- 未来: `OpenAiProvider`、`AnthropicProvider` 等，只需实现同一接口

**关键设计原则**：
1. **接口在业务包中**，实现在独立的 `provider` 子包，未来新增 provider 不改动业务代码
2. **模型配置与 provider 解耦**：`LlmModel` 实体记录 provider 类型、模型 ID、API key、base URL，新增 provider 只需在数据库加配置
3. **工厂模式**：`LlmProviderFactory` 根据配置中的 `provider_type` 字段动态实例化对应 provider
4. **统一错误处理**：各 provider 的限流/超时/认证错误统一转换为 `LlmException`，业务层不感知具体 provider
5. **统一超时配置**：所有 provider 共享同一个超时配置，避免各 provider 超时行为不一致
6. **场景路由**：为不同使用场景（analysis/writing/general）分配不同模型，未配置场景回退到默认模型

接口定义在 `service.llm` 包，实现在 `service.llm.provider` 子包。

### D6: 外部 API 调用策略

arXiv API 和 Semantic Scholar API 均为免费公开 API，有速率限制：
- arXiv: 建议每 3 秒一次请求，Atom XML 格式分页（每页 10 条）
- Semantic Scholar: 免费层 100 次/5 分钟

**单次搜索上限**: 每个数据源最多获取 100 条结果（arXiv: 10 页 × 10 条，请求间隔 ≥3s），避免短时间内大量请求。

**幂等存储**: paper 表按 `project_id + arxiv_id` 维度存储，`UNIQUE(project_id, arxiv_id)` 约束保证同一项目内不重复插入。

**策略**: 检索时通过后端缓存 + 分页控制，避免短时间内大量请求。LLM 分析使用异步任务，不阻塞 HTTP 请求。

### D6.1: 多阶段 LLM 分析架构

分析质量优先于速度和成本。采用 **逐篇理解 → 交叉对比 → 创新生成** 三阶段架构，确保每个创新点都有证据支撑。

```
触发分析 (N 篇论文, N ≤ 20)
  │
  ▼ 阶段 1: 逐篇深度理解 (N 次 LLM 调用, 并行)
  ├─ PaperAnalysis[0]: coreQuestion, methodology, contributions[], limitations[], keywords[], methodsAndDatasets
  ├─ PaperAnalysis[1]: ...
  ├─ ...
  └─ PaperAnalysis[N-1]: ...
     │ (全部完成后进入阶段 2)
     ▼ 阶段 2: 交叉对比分析 (1 次 LLM 调用)
     输入: N 篇 PaperAnalysis 的 JSON 数组
     输出: topicClusters[], methodComparison[], conflicts[], consensusPoints[], methodologyGaps[], datasetGaps[]
        │ (完成后进入阶段 3)
        ▼ 阶段 3: 创新机会生成 (1 次 LLM 调用)
        输入: 阶段 2 交叉分析结果 + 原始论文引用
        输出: gaps[], basePapers[], innovationPts[]
           │
           ▼ 保存 analysis_result 到 DB
           ▼ WebSocket 推送 ANALYZED

总计 LLM 调用: N + 2 次
20 篇论文 = 22 次调用
```

**阶段 1 Prompt 结构** (每篇论文独立调用):
```
你是一位资深学术研究顾问。请深度分析以下论文，提取关键信息：

Title: {title}
Abstract: {abstract}
Citations: {count}

请以 JSON 格式返回：
{
  "coreQuestion": "该论文要解决的核心问题 (1句话)",
  "methodologyCategory": "theoretical | experimental | survey | comparative",
  "keyContributions": ["贡献1", "贡献2", "贡献3"],
  "limitations": ["局限1", "局限2", "局限3"],
  "keywords": ["关键词1", ...],
  "methodsAndDatasets": ["使用的具体方法/模型/数据集"]
}
```

**阶段 2 Prompt 结构** (交叉分析):
```
以下是 {N} 篇论文的深度分析结果。请进行交叉对比：

[论文摘要列表...]

请以 JSON 格式返回：
{
  "topicClusters": [{"name": "簇名", "paperIndices": [0,1,2]}],
  "methodComparison": [{"method": "方法名", "papers": [0,3]}],
  "conflicts": [{"description": "矛盾点", "paperA": 0, "paperB": 2}],
  "consensusPoints": ["共识1", "共识2"],
  "methodologyGaps": ["没人用的方法"],
  "datasetGaps": ["没人覆盖的数据集"]
}
```

**阶段 3 Prompt 结构** (创新生成):
```
基于以下交叉分析结果，请生成研究空白、base 论文推荐和创新点：

[交叉分析结果...]
[原始论文信息...]

注意：
- 每个研究空白必须基于交叉分析的具体证据，不得凭空编造
- base 论文优先选择引用数高、方法通用性强的论文
- 创新点必须具体可执行，不得泛泛而谈
- 每个创新点必须关联到交叉分析中的具体缺口

{JSON Schema 定义}
```

**并行化策略**:
- 阶段 1 的 N 次 LLM 调用完全独立，使用 `@Async` + `CompletableFuture.allOf()` 并行执行
- 线程池配置：核心线程数 5，最大线程数 10，队列容量 20（确保 LLM 分析有足够并发能力）
- 单篇分析失败不影响整体流程，标记为失败后跳过，最终报告中标注缺失篇数

**预计耗时** (20 篇论文):
- 阶段 1: ~10-20 秒 (5 并发，每篇约 2-4 秒)
- 阶段 2: ~3-5 秒
- 阶段 3: ~5-10 秒
- 总计: ~20-40 秒

### D6.2: 论文全文提取 (Chain of Responsibility)

LLM 分析需要论文全文信息，尤其是公式和图表。由于论文来源的多样性（arXiv PDF、LaTeX 源码、Semantic Scholar 元数据等），采用 **责任链模式** 设计 5 层回退提取策略，按信息完整性优先级排序：

```
┌─────────────────────────────────────────────────────────────┐
│              PaperFullTextExtractor (责任链)                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. LaTeXExtractor                                           │
│     └─ arXiv .tar.gz → 解压 → 提取 .tex 文本                │
│     └─ 保留公式源码 ($...$, \begin{equation})                │
│     └─ 有效性: textLength ≥ 500                             │
│          ↓ 失败                                               │
│  2. VisionExtractor                                          │
│     └─ PDF → PNG (300 DPI) → qwen2.5-vl 视觉模型            │
│     └─ 保留图表/公式视觉信息                                  │
│     └─ 有效性: pages ≥ 3                                    │
│          ↓ 失败                                               │
│  3. Ar5ivExtractor                                           │
│     └─ ar5iv HTML → 提取文本 (公式转 MathJax)                │
│     └─ 图表丢失，公式结构保留                                 │
│          ↓ 失败                                               │
│  4. PdfBoxExtractor                                          │
│     └─ PDFBox 提取纯文本                                     │
│     └─ 公式乱码，图表丢失                                     │
│          ↓ 失败                                               │
│  5. AbstractExtractor                                        │
│     └─ 返回摘要作为回退                                      │
│     └─ 标记 "缺少全文"                                       │
│                                                             │
│  返回: PaperFullText(fullText, strategy, textLength, warnings)│
└─────────────────────────────────────────────────────────────┘
```

**接口定义**:
```java
public interface PaperFullTextExtractor {
    PaperFullText extractFullText(Paper paper);
    String getStrategyName();
    void setNext(PaperFullTextExtractor next);
}

public record PaperFullText(
    String fullText,
    String strategy,      // 使用的策略名称
    int textLength,
    String warnings       // 多警告信息逗号分隔
) {}
```

**配置项** (`application.yml`):
```yaml
paper:
  fulltext:
    extractor-order:  # 责任链顺序，未配置时使用默认值
      - latex         # 1. LaTeX 源码提取 (arXiv .tar.gz)
      - vision        # 2. 视觉模型提取 (PDF→PNG→qwen2.5-vl)
      - ar5iv         # 3. ar5iv HTML 转换 (公式转 MathJax)
      - pdfbox        # 4. PDFBox 纯文本提取
      - abstract      # 5. 摘要回退 (最后兜底)
```

**每个策略的 Javadoc 要求**:
- `@see` 引用的相关枚举/常量
- 策略的优缺点说明（信息完整性、性能成本、适用场景、限制条件）
- 有效性阈值说明

**策略对比表**:

| 策略 | 优点 | 缺点 | 适用场景 | 成本 |
|------|------|------|----------|------|
| LaTeX | 公式最完整 (保留 LaTeX 源码)、图表信息完整 | 仅 arXiv 论文可用，需下载解压 .tar.gz | 有源码的 arXiv 论文 | 低 (仅网络+解压) |
| Vision | 保留图表和公式的视觉呈现、支持任意 PDF | LLM 调用成本高、处理速度慢 (20 页论文约 5-10 秒) | 无源码但有 PDF 的论文 | 高 (qwen2.5-vl token 消耗) |
| ar5iv | 公式保留 MathJax 结构、文本质量好 | 图表丢失、ar5iv 不支持所有 arXiv 论文 | arXiv 但无 LaTeX 源码 | 低 (HTTP 请求) |
| PDFBox | 纯文本提取速度快、无需网络 | 公式乱码、图表丢失、PDF 解析不稳定 | 任何 PDF 论文 (兜底) | 低 (本地处理) |
| Abstract | 始终可用、零成本 | 信息严重不足、无法深度分析 | 所有全文提取均失败 | 零 |

**设计原则**:
1. **信息完整性优先**: 公式和图表对分析质量至关重要，LaTeX 源码最完整
2. **可配置链顺序**: 策略顺序通过配置决定，默认按信息完整性排序
3. **每层独立验证**: 各策略有最小内容长度阈值，低于阈值视为提取失败
4. **可独立测试**: 每个 extractor 可单独注入和测试
5. **未来可扩展**: 新增策略只需实现接口并加入配置列表
6. **策略可观测**: 每次提取记录策略名称和文本长度，用于分析质量评估
7. **文档化**: 每个策略实现时必须包含详细 Javadoc，说明优缺点

### D7: 异步任务管理

论文检索和 LLM 分析使用 Spring 的 `@Async` + `CompletableFuture` 在后台线程池执行。任务进度通过 WebSocket STOMP 实时推送。任务状态存储在数据库中，页面刷新后可恢复。

**任务 ID 机制**:
- 使用 `UUID.randomUUID().toString()` 生成 taskId
- taskId 作为 WebSocket topic: `/topic/task/{taskId}`
- 前端通过 `GET /api/v1/papers/tasks/{taskId}` 或 `GET /api/v1/analysis/tasks/{taskId}` 查询任务状态
- 任务状态持久化到 `task_status` 数据库表，应用重启不丢失，页面刷新后可恢复

**WebSocket 架构**: Spring STOMP over WebSocket，前端订阅 `/topic/task/{taskId}` 接收进度推送。STOMP 内置心跳和自动重连机制，无需手动实现。

**线程池配置**: 
- 论文检索: 核心线程数 2，最大线程数 5，队列容量 10。避免过多并发请求外部 API 触发限流。
- LLM 分析: 核心线程数 5，最大线程数 10，队列容量 20。确保阶段 1 的 N 次并行 LLM 调用有足够并发能力。

### D7.1: 定时任务 (web 模块内的 scheduler 包)

MVP 阶段定时任务作为 web 模块内的 `scheduler` 包实现，`@Scheduled` 直接通过 Spring 注入调用业务执行器。**保留内部 HTTP API 端点**供未来 XXL-JOB 等分布式调度系统远程调用。

```
┌─────────────────────────────────────────────────────────┐
│         paper-assistant-web (单一 Spring Boot 应用)      │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  scheduler 包 (同模块)                             │  │
│  │                                                   │  │
│  │  @Scheduled (cron触发器)                           │  │
│  │       │                                           │  │
│  │       ▼                                           │  │
│  │  SchedulerJob                                      │  │
│  │  (注入 SchedulerJobExecutor)                       │  │
│  └───────────────────────────┬───────────────────────┘  │
│                              │ Spring DI                │
│                              ▼                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │  scheduler 包                                      │  │
│  │                                                   │  │
│  │  SchedulerJobExecutor (业务执行器接口)             │  │
│  │  - TaskStatusCleanupExecutor                      │  │
│  │       │                                           │  │
│  │       ▼                                           │  │
│  │  核心业务 Service 层                               │  │
│  │  (TaskStatusService, ...)                         │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ SchedulerJobController (HTTP API)           │  │  │
│  │  │ POST /api/v1/scheduler/{job-name}/execute   │  │  │
│  │  │ [未来分布式调度远程调用入口]                   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**设计原则**:
1. **MVP 包级别**: 定时任务在 web 模块的 `scheduler` 包中，无需跨模块依赖
2. **触发器与业务解耦**: `@Scheduled` 仅负责触发和记录日志，实际执行逻辑在 `SchedulerJobExecutor`
3. **HTTP API 保留**: `POST /api/v1/scheduler/{job-name}/execute` 作为未来 XXL-JOB 远程调用入口
4. **迁移路径**: 升级为分布式调度时，只需拆分 `scheduler` 为独立模块/进程，通过 HTTP 调用现有 API，业务执行器代码不变
5. **可观测性**: 每次执行记录执行时间、结果、耗时、异常

**目录结构**:
```
paper-assistant-web/
└── src/main/java/com/paperassistant/
    └── scheduler/                # 定时任务包
        ├── config/
        │   └── SchedulerConfig   # @EnableScheduling + cron配置
        ├── job/
        │   ├── SchedulerJob      # 定时任务定义 (@Scheduled)
        │   └── TaskStatusCleanupJob  # task_status 清理
        ├── executor/
        │   ├── SchedulerJobExecutor  # 业务执行器接口
        │   └── TaskStatusCleanupExecutor  # 清理实现
        └── controller/
            └── SchedulerJobController  # HTTP API (未来分布式调度)
```

**配置项** (`application.yml`):
```yaml
scheduler:
  jobs:
    task-status-cleanup:
      cron: "0 0 2 * * ?"        # 默认每天凌晨2点
      enabled: true               # 可单独关闭某个任务
```

**MVP → XXL-JOB 迁移路径**:
```
阶段1 (MVP):  @Scheduled → DI → Executor → Service  (同模块)
阶段2 (XXL):  XXL-JOB Admin → HTTP POST /api/v1/scheduler/{job-name}/execute → Executor → Service
              (拆分scheduler为独立模块/进程, 业务执行器代码不变)
```

### D8: 项目目录结构

```
paper-assistant/
├── pom.xml                           # 单模块 Spring Boot (后续扩展多模块)
├── paper-assistant-web/              # Spring Boot 主应用
│   ├── src/main/java/com/paperassistant/
│   │   ├── config/                   # 配置类 (WebSocket, Async, LLM, Scheduler)
│   │   ├── controller/               # REST 控制器 (外部 API)
│   │   ├── entity/                   # 数据库实体
│   │   ├── mapper/                   # MyBatis-Plus Mapper
│   │   ├── scheduler/                # 定时任务包
│   │   │   ├── config/
│   │   │   │   └── SchedulerConfig  # @EnableScheduling + cron
│   │   │   ├── job/                 # 定时任务 (@Scheduled)
│   │   │   ├── executor/            # 业务执行器
│   │   │   └── controller/          # HTTP API (未来分布式调度)
│   │   ├── service/
│   │   │   ├── retrieval/            # 论文检索
│   │   │   │   ├── PaperRetrievalService
│   │   │   │   ├── ArxivFetcher
│   │   │   │   └── SemanticScholarFetcher
│   │   │   ├── analysis/             # 创新分析
│   │   │   │   ├── InnovationAnalysisService
│   │   │   │   └── PromptTemplates
│   │   │   ├── project/              # 项目管理
│   │   │   │   └── ProjectService
│   │   │   └── llm/                  # LLM 抽象层
│   │   │       ├── LlmProvider       # 接口
│   │   │       ├── DashScopeProvider # 百炼实现
│   │   │       └── LlmService        # 路由+工厂
│   │   └── dto/                      # 数据传输对象
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/             # Flyway 迁移脚本
│   └── pom.xml
├── paper-assistant-frontend/         # Vue 3 前端
│   ├── src/
│   │   ├── views/
│   │   │   ├── Dashboard.vue         # 项目列表
│   │   │   ├── ProjectDetail.vue     # 项目详情 + 论文检索
│   │   │   └── AnalysisReport.vue    # 分析报告
│   │   ├── components/               # 可复用组件
│   │   ├── api/                      # API 调用封装
│   │   └── stores/                   # Pinia 状态管理
│   └── package.json
└── openspec/                         # OpenSpec 规范
```

## Risks / Trade-offs

| 风险/权衡 | 影响 | 缓解措施 |
|-----------|------|----------|
| arXiv/Semantic Scholar API 限流 | 检索失败或返回不完整 | 实现请求队列 + 指数退避重试 + 本地缓存 |
| LLM 输出格式不稳定 | 结构化分析结果解析失败 | 使用 LangChain4j 的结构化输出 + JSON Schema 验证 + 失败重试 |
| 论文去重仅按标题相似度 | 可能误判或漏判 | Levenshtein 0.15 为初值，后续可加入作者/年份联合判断 |
| 无用户认证 | Round 1 所有数据对所有请求可见 | 接受此限制，明确标注为 MVP 范围，后续 Round 添加 Spring Security |
| LLM 分析成本高 | 大批量分析消耗 token | 限制单次分析论文数量上限 (20 篇)，使用 qwen-plus 而非 qwen-max |

## 基础设施约定

### 统一 API 响应格式

所有 REST 接口返回 `ApiResponse<T>`：
```java
{
  "code": 200,           // 业务状态码
  "message": "success",  // 人类可读消息
  "data": { ... },       // 业务数据
  "timestamp": 1713456789000
}
```

### 统一异常处理

- 业务异常通过 `BusinessException` 抛出，携带 `ErrorCode` 枚举
- `GlobalExceptionHandler` (@ControllerAdvice) 捕获所有异常，转换为 `ApiResponse`
- 异常码分类：参数错误 (400x) / 资源不存在 (404x) / 外部API失败 (502x) / 限流 (503x) / 内部错误 (500x)
- 前端通过 `code` 判断，不依赖 HTTP 状态码

### 统一日志处理

- `logback-spring.xml` 配置多环境输出 (dev: console彩色 / prod: JSON格式)
- 每个请求通过 MDC 注入 `traceId`，日志格式: `[traceId] [level] [class] [method] message`
- HTTP 请求拦截器记录: method, path, query, 耗时, 响应码, traceId
- 文件滚动策略: 按天滚动，最大 50MB，保留 30 天

### WebSocket 配置

使用 Spring STOMP over WebSocket 协议。

- Broker 端点: `/ws` (WebSocket 连接端点)
- 客户端订阅: `/topic/task/{taskId}` 接收任务进度
- 服务端推送: `simpMessagingTemplate.convertAndSend("/topic/task/" + taskId, message)`
- 消息体: `{ stage: "SEARCHING" | "SEARCHED" | "FAILED", progress: 0-100, message: "正在查询...", data: {...} }`
- 前端: `@stomp/stompjs` 库连接，内置自动重连和心跳
- STOMP 心跳配置: 发送间隔 30s，接收间隔 30s，心跳超时触发断线重连
- 降级方案: WebSocket 不可用时，前端使用 HTTP 轮询 `GET /api/v1/papers/tasks/{taskId}` 或 `GET /api/v1/analysis/tasks/{taskId}`

### 跨域配置

- Spring CORS 配置: 允许 `http://localhost:5173` (Vite dev server)
- 允许的 HTTP 方法: GET, POST, PUT, DELETE, OPTIONS
- 允许的 Header: Content-Type, Authorization
- 生产环境通过 Nginx 反向代理解决跨域，不依赖 CORS

### 参数校验

- 使用 `spring-boot-starter-validation` (JSR 303 / Bean Validation)
- Controller 入参使用 `@Valid` + DTO，校验失败由 `GlobalExceptionHandler` 捕获 `MethodArgumentNotValidException`
- 实体字段校验示例: `@NotBlank` (项目名不能为空), `@Size` (关键词长度限制)

### 项目状态机

项目状态转换遵循严格的状态机约束：

```
CREATED → SEARCHING → SEARCHED → ANALYZING → ANALYZED
  ↑                                          |
  └──────────────────────────────────────────┘
  (可重新开始新一轮检索+分析)
```

- 只有 SEARCHED 状态的项目才能触发分析
- 只有 CREATED 或 SEARCHED 状态的项目才能触发检索
- 非法状态转换返回 `ErrorCode.STATE_TRANSITION_INVALID`

### 实体基类

- 所有实体类继承 `BaseEntity`，统一包含: `id` (BIGINT 主键), `createTime`, `updateTime`
- MyBatis-Plus 注解: `@TableId(type = IdType.ASSIGN_ID)`, `@TableField(fill = FieldFill.INSERT)`, `@TableField(fill = FieldFill.INSERT_UPDATE)`
- 配合 `MetaObjectHandler` 自动填充时间字段

### 数据库配置

- HikariCP 连接池 (Spring Boot 默认):
  - 最大连接数: 20
  - 最小空闲连接: 5
  - 连接超时: 30s
  - 空闲超时: 600s
- 多环境配置: `application-dev.yml` (连接本地 Docker PG/Redis, 开启 SQL 日志) / `application-prod.yml` (环境变量注入, 关闭 SQL 日志)
- 默认激活 profile: `dev`

### Flyway 数据库迁移管理

- SQL 脚本目录: `src/main/resources/db/migration/`
- 命名规范: `V{版本号}__{描述}.sql` (如 `V1__init.sql`, `V2__add_llm_config.sql`)
- 版本号单调递增，已执行的脚本不可修改，只能新增
- 配置:
  - `baseline-on-migrate: true` (已有库不破坏现有数据)
  - `validate-on-migrate: true` (脚本校验不通过则启动失败)
  - 失败自动回滚 (每个脚本用事务包裹)
- 本地开发: 应用启动时自动执行未运行的迁移脚本
- 回滚策略: 手动编写 `V{版本号}__UNDO__描述.sql` 进行回滚
- V1 迁移中包含 INSERT 语句初始化默认 LLM 配置 (DashScope, qwen-plus)

### V1 迁移表结构定义

**project 表**:
```sql
id            BIGINT PRIMARY KEY,
name          VARCHAR(255) NOT NULL,
description   TEXT,
topic         VARCHAR(500) NOT NULL,
status        VARCHAR(32) NOT NULL DEFAULT 'CREATED',  -- CREATED/SEARCHING/SEARCHED/ANALYZING/ANALYZED
base_paper_id BIGINT,      -- 关联选中的 base 论文
is_deleted    SMALLINT NOT NULL DEFAULT 0,  -- 逻辑删除: 0-未删除, 1-已删除
create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
```

**paper 表**:
```sql
id               BIGINT PRIMARY KEY,
project_id       BIGINT NOT NULL,     -- 逻辑关联, 通过 project.is_deleted 判断
arxiv_id         VARCHAR(128),
title            VARCHAR(1000) NOT NULL,
abstract         TEXT,
authors          JSONB,               -- 作者姓名 JSON 数组
publish_date     DATE,
citation_count   INT DEFAULT 0,
influence_score  FLOAT DEFAULT 0,     -- MVP 阶段 = citation_count
has_code         BOOLEAN DEFAULT FALSE,
code_url         VARCHAR(500),
pdf_url          VARCHAR(500),
is_deleted       SMALLINT NOT NULL DEFAULT 0,
create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
UNIQUE (project_id, arxiv_id)         -- 项目内幂等约束
```

**analysis_result 表**:
```sql
id            BIGINT PRIMARY KEY,
project_id    BIGINT NOT NULL,
gaps          JSONB,       -- 研究空白列表
base_papers   JSONB,       -- 推荐 base 论文列表 (含推荐理由、创新方向)
innovation_pts JSONB,      -- 创新点列表
is_deleted    SMALLINT NOT NULL DEFAULT 0,
create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
```

**llm_config 表**:
```sql
id            BIGINT PRIMARY KEY,
name          VARCHAR(100) NOT NULL,
provider_type VARCHAR(32) NOT NULL,  -- dashscope/openai/anthropic
base_url      VARCHAR(500),
api_key       VARCHAR(500) NOT NULL,  -- 加密存储
enabled       BOOLEAN DEFAULT TRUE,
is_deleted    SMALLINT NOT NULL DEFAULT 0,
create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
```

**llm_model 表**:
```sql
id            BIGINT PRIMARY KEY,
config_id     BIGINT NOT NULL,
model_id      VARCHAR(100) NOT NULL,  -- 如 qwen-max
display_name  VARCHAR(200) NOT NULL,
capabilities  VARCHAR(255),            -- 逗号分隔: analysis,writing,general
max_tokens    INT,
enabled       BOOLEAN DEFAULT TRUE,
sort_order    INT DEFAULT 0,
is_deleted    SMALLINT NOT NULL DEFAULT 0,
create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
```

**task_status 表**:
```sql
id            BIGINT PRIMARY KEY,
task_id       VARCHAR(64) NOT NULL UNIQUE,  -- UUID
task_type     VARCHAR(32) NOT NULL,          -- SEARCH / ANALYSIS
project_id    BIGINT NOT NULL,
status        VARCHAR(32) NOT NULL,          -- SEARCHING/SEARCHED/FAILED 或 ANALYZING/ANALYZED/FAILED
progress      INT NOT NULL DEFAULT 0,        -- 0-100
stage         VARCHAR(64),                   -- 当前阶段描述
message       TEXT,                          -- 进度消息/错误信息
result_data   JSONB,                         -- 任务完成后的结果数据
create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
```

**逻辑删除策略**: project、paper、analysis_result、llm_config、llm_model 使用 MyBatis-Plus `@TableLogic` 实现逻辑删除。删除 project 时，应用层手动标记关联的 paper 和 analysis_result 为已删除（不使用 ON DELETE CASCADE）。task_status 表不使用逻辑删除，通过定时任务清理过期记录。所有查询自动过滤 `is_deleted = 0` 的记录。

### MyBatis-Plus 配置

- 分页插件: `PaginationInnerInterceptor` (PostgreSQL 方言)
- 逻辑删除插件: `LogicalDeleteInnerInterceptor` (is_deleted: 0→未删除, 1→已删除)
- 乐观锁插件 (后续需要时启用)
- 开发环境 SQL 日志: `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl` (dev profile)

### Jackson 序列化

- 时区: `Asia/Shanghai`
- 日期格式: `yyyy-MM-dd HH:mm:ss`
- null 值处理: 不序列化 null 字段 (减少响应体积)

### 前端技术设计

#### F1: 项目目录结构

```
paper-assistant-frontend/
├── index.html
├── vite.config.ts                    # Vite 配置 (代理、插件、别名)
├── tsconfig.json
├── package.json
└── src/
    ├── main.ts                       # 应用入口 (初始化 Pinia/Router/StompManager)
    ├── App.vue                       # 根组件 (router-view + 全局布局)
    ├── router/
    │   └── index.ts                  # Vue Router 路由定义
    ├── api/
    │   ├── request.ts                # axios 实例 + 统一拦截器
    │   ├── project.ts                # 项目管理 API
    │   ├── paper.ts                  # 论文检索 API
    │   ├── analysis.ts               # 分析 API
    │   └── task.ts                   # 任务状态 API
    ├── stores/
    │   ├── project.ts                # Pinia project store
    │   ├── paper.ts                  # Pinia paper store
    │   └── analysis.ts               # Pinia analysis store
    ├── types/
    │   ├── api.ts                    # ApiResponse, ErrorCode 等通用类型
    │   ├── project.ts                # ProjectDTO, ProjectStatus 等
    │   ├── paper.ts                  # PaperDTO, SortOrder 等
    │   └── analysis.ts               # AnalysisResultDTO, ResearchGap 等
    ├── utils/
    │   ├── format.ts                 # 日期/数字格式化
    │   ├── status.ts                 # 状态标签映射 (中文) + 操作禁用判断
    │   └── websocket.ts              # STOMP 连接管理 (StompManager 单例)
    ├── components/
    │   ├── AppHeader.vue             # 全局顶部导航栏
    │   ├── BreadcrumbNav.vue         # 面包屑导航组件
    │   ├── EmptyState.vue            # 通用空状态组件
    │   ├── TaskProgressPanel.vue     # 内嵌任务进度面板
    │   ├── PaperDetailDrawer.vue     # 论文详情抽屉 (右侧滑出)
    │   ├── BasePaperSelector.vue     # Base 论文选择面板
    │   ├── PaperTable.vue            # 论文列表表格 (可复用)
    │   └── StateTag.vue              # 项目状态标签 (颜色+文字)
    ├── views/
    │   ├── Dashboard.vue             # 项目列表页
    │   ├── ProjectDetail.vue         # 项目详情页
    │   └── AnalysisReport.vue        # 分析报告页
    └── assets/
        └── styles/
            └── global.css            # 全局样式 (Element Plus 主题覆盖)
```

#### F2: TypeScript 类型系统

```typescript
// types/api.ts — 通用类型
interface ApiResponse<T> {
  code: number;         // 200=成功, 400x=参数错误, 500x=内部错误
  message: string;
  data: T;
  timestamp: number;
}

enum ErrorCode {
  PARAM_INVALID = 4001,
  RESOURCE_NOT_FOUND = 4041,
  EXTERNAL_API_FAILED = 5021,
  RATE_LIMITED = 5031,
  LLM_ERROR = 5032,
  INTERNAL_ERROR = 5001,
  STATE_TRANSITION_INVALID = 4002,
}

// types/project.ts — 项目类型
enum ProjectStatus {
  CREATED = 'CREATED',       // 已创建
  SEARCHING = 'SEARCHING',   // 检索中
  SEARCHED = 'SEARCHED',     // 检索完成
  ANALYZING = 'ANALYZING',   // 分析中
  ANALYZED = 'ANALYZED',     // 分析完成
}

interface ProjectDTO {
  id: number;
  name: string;
  description: string | null;
  topic: string;
  status: ProjectStatus;
  basePaperId: number | null;
  basePaperTitle: string | null;
  createTime: string;
  updateTime: string;
  paperCount: number;           // 该项目关联的论文数 (通过 COUNT 查询)
}

// types/paper.ts — 论文类型
type SortOrder = 'relevance' | 'citation' | 'date';

interface PaperDTO {
  id: number;
  projectId: number;
  arxivId: string | null;
  title: string;
  abstract: string | null;
  authors: string[];
  publishDate: string | null;
  citationCount: number;
  influenceScore: number;
  hasCode: boolean;
  codeUrl: string | null;
  pdfUrl: string | null;
  category: string | null;
}

// types/analysis.ts — 分析类型
interface ResearchGap {
  category: string;              // 未探索/矛盾结论/方法学局限/数据集不足
  description: string;
  evidence: string;
  supportingPapers: Array<{ arxivId: string; title: string }>;
}

interface BasePaperRecommendation {
  paperId: number;
  arxivId: string;
  title: string;
  authors: string[];
  citationCount: number;
  reason: string;
  innovationDirection: string;
}

interface InnovationPoint {
  id: string;
  description: string;
  difficulty: 'low' | 'medium' | 'high';
  contributionType: string;      // 方法改进/新应用/效率优化
  basePaperId: number;
  supportingGap: string;
}

interface AnalysisResultDTO {
  id: number;
  projectId: number;
  gaps: ResearchGap[];
  basePapers: BasePaperRecommendation[];
  innovationPts: InnovationPoint[];
  createTime: string;
}
```

#### F3: API 请求层

- **axios 实例**: `baseURL: '/api/v1'`, `timeout: 30000`
- **响应拦截器**: 解析 `ApiResponse.code !== 200` 时自动 `ElMessage.error()`
- **错误拦截器**: 网络异常时显示 "网络连接失败，请重试"
- **模块划分**: `api/project.ts`, `api/paper.ts`, `api/analysis.ts`, `api/task.ts` 各自导出对应模块 API 函数

#### F4: Pinia Store 设计

**project store** (`stores/project.ts`):
- state: `projects: ProjectDTO[]`, `currentProject: ProjectDTO | null`, `loading: boolean`
- actions: `fetchList()`, `fetchById(id)`, `create(data)`, `update(data)`, `remove(id)`, `setCurrent(project)`, `setBasePaper(projectId, paperId)`

**paper store** (`stores/paper.ts`):
- state: `papers: PaperDTO[]`, `total: number`, `currentPage: number`, `pageSize: number`, `sortOrder: SortOrder`, `loading: boolean`, `selectedIds: Set<number>`, `currentTaskId: string | null`, `currentTaskStatus: TaskStatusDTO | null`
- actions: `search(keyword)`, `fetchList(params)`, `selectPaper(id)`, `deselectPaper(id)`, `clearSelection()`, `fetchTaskStatus(taskId)`

**analysis store** (`stores/analysis.ts`):
- state: `result: AnalysisResultDTO | null`, `currentTaskId: string | null`, `currentTaskStatus: TaskStatusDTO | null`, `loading: boolean`
- actions: `trigger(projectId)`, `fetchResult(projectId)`, `fetchTaskStatus(taskId)`

#### F5: WebSocket 连接管理

`utils/websocket.ts` — `StompManager` 单例类:
- 应用启动时建立 WebSocket 连接 (`main.ts` 中调用 `connect()`)
- STOMP 心跳: 发送 30s，期望接收 30s
- 断开时自动重连，间隔 3s，最多重试 5 次
- 5 次后自动降级为 HTTP 轮询 (`GET /api/v1/*/tasks/{taskId}`)
- API: `connect()`, `disconnect()`, `subscribe(taskId, callback)`, `unsubscribe(taskId)`, `isConnected()`

#### F6: 通用组件设计

> **UI 设计约束**: 所有组件样式必须严格复用 `html-mockups/` 目录下的 HTML mockup 设计。以下为组件结构和交互定义，具体视觉样式以 mockup 为准。

| 组件 | 用途 | Props | 参考 mockup |
|------|------|-------|-------------|
| `AppHeader` | 全局顶部导航栏 (56px, sticky, 白色背景, 底部 1px 边框) | — | 01-dashboard |
| `BreadcrumbNav` | 面包屑导航 (`items: Array<{ label: string; to?: string }>`, 最后一项不可点击, 分隔符 `/`) | `items` | 02-project-detail |
| `EmptyState` | 通用空状态 (`title`, `description`, `actionText?`, `onAction?`) | `title`, `description` | Dashboard |
| `TaskProgressPanel` | 内嵌任务进度面板 (标题 + taskId 等宽字体 + 进度条 6px + 阶段描述 + 百分比) | `taskId`, `taskType`, `autoSubscribe?` | 02-project-detail |
| `PaperDetailDrawer` | 论文详情右侧滑出抽屉 (540px, sticky 头部, 标题 → meta → 摘要 → 链接按钮) | `paper: PaperDTO \| null`, `visible: boolean` | 02-project-detail |
| `BasePaperSelector` | Base 论文选择面板 (modal 620px, 自定义 radio, 标题 + 作者/年份/引用, 底部取消/确认) | `papers: PaperDTO[]`, `currentBasePaperId` | 02-project-detail |
| `PaperTable` | 论文列表表格 (toolbar 左侧已选数量 + 右侧 sort tabs pill, 表头灰色背景 uppercase, 行 hover/selected 高亮) | `papers`, `selectable?`, `selectedIds` | 02-project-detail |
| `StateTag` | 项目状态标签 (pill 999px, 内含 6px 圆点, 5 种状态配色, SEARCHING/ANALYZING 圆点 pulse 动画) | `status: ProjectStatus` | 01-dashboard |

**按钮样式**: primary (accent 蓝背景白字), ghost (透明), outline (1px 边框), danger (hover 变红), purple (purple 背景), sm 尺寸 (padding 5px 12px)。

**研究空白分类 pill 配色**: 未探索领域 (blue), 矛盾结论 (amber), 方法学局限 (purple), 数据集不足 (pink)。

**创新点卡片**: 顶部 3px 色条 (低难度=绿/中难度=橙/高难度=红), 难度标签 + 贡献类型标签。

**响应式断点**: `max-width: 1024px` (padding 20px, 表格横向滚动), `max-width: 768px` (单列, 字号缩小)。

#### F7: 状态驱动的操作控制

```typescript
// utils/status.ts
function getProjectStatusInfo(status: ProjectStatus): {
  label: string;       // 中文标签
  color: string;       // Element Plus Tag 颜色
  disabledActions: string[];  // 该状态下禁用的操作
}

// 各状态禁用操作映射
const disabledActionsByStatus: Record<ProjectStatus, string[]> = {
  CREATED:    ['triggerAnalysis', 'setBasePaper'],
  SEARCHING:  ['search', 'triggerAnalysis', 'setBasePaper'],
  SEARCHED:   [],
  ANALYZING:  ['search', 'triggerAnalysis', 'setBasePaper'],
  ANALYZED:   [],
};
```

#### F8: Vite 开发服务器代理

```typescript
// vite.config.ts
server: {
  port: 5173,
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true },
    '/ws':  { target: 'ws://localhost:8080', ws: true },
  },
}
```

## Open Questions

1. **数据集管理**: Round 1 不涉及代码复现，但论文数据（如检索结果）的缓存策略和有效期未确定。建议：检索结果缓存 24 小时，过期后自动重新请求。
2. **LLM Prompt 优化**: Prompt 模板的管理方式（硬编码 vs 数据库存储 vs 文件）需要在实现时决定。建议：Round 1 先用硬编码 + 常量类，后续迁移到数据库以支持动态更新。
3. **前端国际化**: Round 1 是否需要多语言支持？建议：暂不需要，全中文界面。
