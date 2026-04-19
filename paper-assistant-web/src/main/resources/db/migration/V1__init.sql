-- V1__init.sql
-- 初始化所有核心表

-- 项目表
CREATE TABLE IF NOT EXISTS project (
    id text PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    topic VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    base_paper_id text,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 论文表
CREATE TABLE IF NOT EXISTS paper (
    id text PRIMARY KEY,
    project_id text NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'arxiv',
    source_id VARCHAR(255),
    arxiv_id VARCHAR(128),
    title VARCHAR(1000) NOT NULL,
    abstract TEXT,
    authors JSONB,
    publish_date DATE,
    citation_count INT DEFAULT 0,
    influence_score FLOAT DEFAULT 0,
    has_code BOOLEAN DEFAULT FALSE,
    code_url VARCHAR(500),
    pdf_url VARCHAR(500),
    category VARCHAR(128),
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_project_source_external UNIQUE (project_id, source, source_id)
);
CREATE INDEX idx_paper_project ON paper (project_id, is_deleted);
CREATE INDEX idx_paper_project_source ON paper (project_id, source);

-- 分析结果表
CREATE TABLE IF NOT EXISTS analysis_result (
    id text PRIMARY KEY,
    project_id text NOT NULL,
    gaps JSONB,
    base_papers JSONB,
    innovation_pts JSONB,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_analysis_result_project ON analysis_result (project_id, is_deleted);

-- LLM 配置表
CREATE TABLE IF NOT EXISTS llm_config (
    id text PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    base_url VARCHAR(500),
    api_key VARCHAR(500) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- LLM 模型表
CREATE TABLE IF NOT EXISTS llm_model (
    id text PRIMARY KEY,
    config_id BIGINT NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    capabilities VARCHAR(255),
    max_tokens INT,
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 任务状态表
CREATE TABLE IF NOT EXISTS task_status (
    id text PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    task_type VARCHAR(32) NOT NULL,
    project_id text NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    stage VARCHAR(64),
    message TEXT,
    result_data JSONB,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_task_status_project ON task_status (project_id);
CREATE INDEX idx_task_status_create ON task_status (create_time);

-- 插入默认 LLM 配置 (DashScope, qwen-plus)
INSERT INTO llm_config (id, name, provider_type, base_url, api_key, enabled)
VALUES ('1', 'DashScope', 'dashscope', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'placeholder', TRUE);

INSERT INTO llm_model (id, config_id, model_id, display_name, capabilities, max_tokens, enabled, sort_order)
VALUES ('1', 1, 'qwen-plus', 'Qwen Plus', 'analysis,writing,general', 8192, TRUE, 0);
