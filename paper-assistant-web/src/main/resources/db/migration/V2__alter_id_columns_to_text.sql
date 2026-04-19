-- 将 ID 相关列从 BIGINT 改为 TEXT，避免前端 JS 精度丢失
-- Java 实体和 DTO 中所有 ID 字段已改为 String 类型，DB 需同步

ALTER TABLE project ALTER COLUMN id TYPE text;
ALTER TABLE project ALTER COLUMN base_paper_id TYPE text;

ALTER TABLE paper ALTER COLUMN id TYPE text;
ALTER TABLE paper ALTER COLUMN project_id TYPE text;

ALTER TABLE analysis_result ALTER COLUMN id TYPE text;
ALTER TABLE analysis_result ALTER COLUMN project_id TYPE text;

ALTER TABLE task_status ALTER COLUMN id TYPE text;
ALTER TABLE task_status ALTER COLUMN project_id TYPE text;

-- 注意：paper_full_text 表无 ID 关联字段，无需修改
-- 注意：所有外键约束和索引会自动迁移到新类型，无需额外操作
