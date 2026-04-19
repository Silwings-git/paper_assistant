-- 添加 source 和 source_id 列，支持多源论文去重
-- 原有 uk_project_arxiv 约束仅适用于 arxiv 来源的论文
-- 新约束 uk_project_source_external 适用于所有来源

-- 添加新列
ALTER TABLE paper ADD COLUMN IF NOT EXISTS source VARCHAR(32) DEFAULT 'arxiv';
ALTER TABLE paper ADD COLUMN IF NOT EXISTS source_id VARCHAR(255);

-- 填充已有数据的 source 字段（都是 arxiv）
UPDATE paper SET source = 'arxiv' WHERE source IS NULL;

-- 删除旧的唯一约束
ALTER TABLE paper DROP CONSTRAINT IF EXISTS uk_project_arxiv;

-- 添加新的唯一约束：project_id + source + source_id
-- source_id 允许为 NULL，但 arxiv 论文始终有 source_id
ALTER TABLE paper ADD CONSTRAINT uk_project_source_external
    UNIQUE (project_id, source, source_id);

-- 更新索引
DROP INDEX IF EXISTS idx_paper_project;
CREATE INDEX idx_paper_project ON paper (project_id, is_deleted);
CREATE INDEX idx_paper_project_source ON paper (project_id, source);
