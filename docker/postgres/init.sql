-- PostgreSQL 初始化脚本
-- 容器首次启动时执行 (仅执行一次)

-- 可以在这里添加额外的初始化逻辑
-- 例如：创建额外的用户、设置权限等

-- ID 列使用 text 类型存储雪花 ID，避免前端 JS 精度丢失
-- (MyBatis-Plus ASSIGN_ID 生成的是 Long，但 Java 实体和 DB 都用 String)
