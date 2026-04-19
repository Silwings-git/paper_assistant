# Paper Assistant

AI 驱动的研究论文助手，帮助研究人员发现最新论文、分析创新机会、复现论文代码、设计和运行实验，以及生成 SCI 论文。

## 功能特性

### Round 1 MVP (当前版本)

- **论文检索**: 支持 arXiv 和 Semantic Scholar 双源检索，结果自动聚合与排序
- **LLM 创新分析**: 三阶段深度分析（逐篇理解 → 交叉对比 → 创新生成），输出研究空白、Base 论文推荐和可创新点
- **论文全文提取**: 责任链模式 5 层回退策略（LaTeX → 视觉模型 → ar5iv → PDFBox → 摘要），最大化提取质量
- **项目管理**: 创建/查看/删除研究项目，绑定研究方向、检索结果和分析报告
- **实时进度推送**: WebSocket STOMP 推送任务进度，支持断线重连与 HTTP 轮询降级
- **Swagger/OpenAPI 文档**: 后端 API 自动文档化，前端 SDK 基于 OpenAPI 规范自动生成

### 后续轮次规划

| 轮次 | 范围 | 独立价值 |
|------|------|---------|
| Round 2 | Rust daemon + gRPC + Docker 容器化 + 代码复现 | 可独立使用，复现论文 |
| Round 3 | 实验设计 + 容器化实验执行 + 结果采集 | 可独立使用，运行实验 |
| Round 4 | LLM 论文撰写 + 结果总结 + 导出 | 完整闭环 |
| Round 5+ | GPU 调度优化 + 多用户 + 前端打磨 | 打磨体验 |

## 技术栈

| 层级 | 技术 |
|-------|-----------|
| 后端 | Java 21 + Spring Boot 3.4 + LangChain4j |
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |
| 数据库 | PostgreSQL + Flyway 迁移 |
| 缓存 | Redis (检索结果缓存) |
| LLM | DashScope (阿里百炼)，多 provider 抽象 |
| ORM | MyBatis-Plus |
| 通信 | REST API + WebSocket (STOMP) |
| 文档 | SpringDoc OpenAPI (Swagger UI) |

## 快速开始

### 前置条件

- Docker + Docker Compose（推荐一键部署）
- 或手动部署: Java 21+ / Node.js 18+ / PostgreSQL 17 / Redis 7
- DashScope API Key

### 方式一: Docker 一键部署

推荐使用 `start.sh` 脚本，见上方 **Docker 一键部署** 章节。

### 方式二: 手动开发环境启动

```bash
# PostgreSQL
docker run -d --name pgsql -e POSTGRES_PASSWORD=root -p 5432:5432 postgres:17

# Redis
docker run -d --name redis -p 6379:6379 redis:7
```

#### 后端

```bash
cd paper-assistant-web

# 设置 DashScope API Key
echo "PAPER_ASSISTANT_LLM_KEY=your_api_key_here" > .env

# 启动开发环境
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后访问 Swagger UI: http://localhost:8080/swagger-ui.html

#### 前端

```bash
cd paper-assistant-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问: http://localhost:5173

### 前端 API 代码生成

当后端接口变更后，重新生成前端 SDK:

```bash
cd paper-assistant-frontend
npm run generate:api   # 下载 OpenAPI 规范 + 生成 TypeScript SDK
```

## Docker 一键部署

项目根目录提供 `start.sh` 脚本，基于 Docker Compose 一键启动所有服务（PostgreSQL + Redis + 后端 + 前端）。

### 使用方式

```bash
# 设置 DashScope API Key
echo "PAPER_ASSISTANT_LLM_KEY=your_api_key_here" > .env

# 一键启动所有服务
./start.sh
```

脚本会自动完成：编译后端 JAR、构建前端、启动 Docker 容器、等待所有服务就绪。

### 命令一览

| 命令 | 说明 |
|------|------|
| `./start.sh` | 启动所有服务（默认，等同 `start`） |
| `./start.sh stop` | 停止所有服务 |
| `./start.sh restart` | 重建并重启所有服务 |
| `./start.sh restart backend` | 仅重启后端（自动重新编译 JAR） |
| `./start.sh restart frontend` | 仅重启前端（自动重新构建） |
| `./start.sh restart postgres` | 仅重启 PostgreSQL |
| `./start.sh restart redis` | 仅重启 Redis |
| `./start.sh rebuild backend` | 重建后端镜像（不重启） |
| `./start.sh status` | 查看服务状态 |
| `./start.sh logs` | 查看所有服务日志 |
| `./start.sh logs backend` | 查看后端日志 |

### Docker Compose 服务

| 服务 | 端口 | 说明 |
|------|------|------|
| postgres | 5432 | PostgreSQL 17（含 pgvector 扩展） |
| redis | 6379 | Redis 7 |
| backend | 8080 | Spring Boot 后端 + Swagger UI |
| frontend | 5173 | Vue 3 前端（Nginx 静态托管） |

## 项目结构

```
paper-assistant/
├── paper-assistant-web/          # Spring Boot 后端
│   ├── controller/               # REST 控制器
│   ├── service/
│   │   ├── retrieval/            # arXiv + Semantic Scholar 检索
│   │   ├── analysis/             # LLM 三阶段创新分析
│   │   ├── project/              # 项目管理
│   │   ├── llm/                  # LLM provider 抽象层
│   │   └── fulltext/             # 论文全文提取 (责任链)
│   ├── scheduler/                # 定时任务 (@Scheduled)
│   └── dto/                      # 数据传输对象
├── paper-assistant-frontend/     # Vue 3 前端
│   ├── views/                    # 页面组件
│   ├── components/               # 可复用组件
│   ├── api/                      # API 层 (自动生成 + 手写封装)
│   ├── stores/                   # Pinia 状态管理
│   └── types/                    # TypeScript 类型定义
├── openspec/                     # OpenSpec 规范文档
└── docs/                         # 架构设计文档
```

## 架构设计

### 整体部署架构

```
┌────────────────────┐
│  Vue SPA            │
│  (Nginx / 静态部署)  │
└──────────┬─────────┘
           │ HTTP/WS
┌──────────▼─────────────────────┐
│  Spring Boot (Java)             │
│  ├─ LLM 抽象层 (DashScope)      │
│  ├─ 业务服务层                  │
│  │  ├─ 论文检索 (arXiv/S2)     │
│  │  ├─ 创新分析 (LLM)          │
│  │  └─ 项目管理                │
│  └─ Rust gRPC 客户端 (预留)     │
│              ┌──────────────┐   │
│              │ PostgreSQL   │   │
│              └──────────────┘   │
└────────────────────────────────┘
```

### LLM 三阶段分析架构

```
触发分析 (N 篇论文)
  │
  ▼ 阶段 1: 逐篇深度理解 (N 次 LLM 调用, 并行)
  ├─ PaperAnalysis[0]: 核心问题、方法、贡献、局限
  ├─ PaperAnalysis[1]: ...
  └─ ...
     │ (全部完成后)
     ▼ 阶段 2: 交叉对比分析 (1 次 LLM 调用)
     输入: N 篇 PaperAnalysis
     输出: 主题聚类、方法对比、矛盾点、共识、缺口
        │
        ▼ 阶段 3: 创新机会生成 (1 次 LLM 调用)
        输出: gaps[], basePapers[], innovationPts[]
```

### 论文全文提取责任链

```
1. LaTeXExtractor    → arXiv .tar.gz 源码 (公式最完整)
     ↓ 失败
2. VisionExtractor   → PDF→PNG→视觉模型 (保留图表公式)
     ↓ 失败
3. Ar5ivExtractor    → ar5iv HTML (公式保留 MathJax)
     ↓ 失败
4. PdfBoxExtractor   → PDFBox 纯文本
     ↓ 失败
5. AbstractExtractor → 摘要兜底
```

完整架构决策见 [docs/architecture.md](docs/architecture.md)。

## 配置说明

### 环境变量

| 变量 | 说明 |
|------|------|
| `PAPER_ASSISTANT_LLM_KEY` | DashScope API Key |

### 数据库连接 (开发环境)

- PostgreSQL: `jdbc:postgresql://localhost:5432/postgres` (用户: postgres, 密码: root)
- Redis: `redis://localhost:6379` (无密码)

### Flyway 迁移

数据库迁移脚本位于 `paper-assistant-web/src/main/resources/db/migration/`，应用启动时自动执行。

## API 文档

后端启动后访问:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

## 许可证

MIT
