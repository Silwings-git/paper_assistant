package com.paperassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paperassistant.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 查询项目列表及关联论文数量
     */
    @Select("""
            SELECT p.*, COALESCE(pc.paper_count, 0) AS paper_count
            FROM project p
            LEFT JOIN (
                SELECT project_id, COUNT(*) AS paper_count
                FROM paper WHERE is_deleted = 0
                GROUP BY project_id
            ) pc ON p.id = pc.project_id
            WHERE p.is_deleted = 0
            ORDER BY p.create_time DESC
            """)
    List<Map<String, Object>> listWithPaperCount();

    /**
     * 查询单个项目的论文数量
     */
    @Select("SELECT COUNT(*) FROM paper WHERE project_id = #{projectId} AND is_deleted = 0")
    int countPapersByProjectId(@Param("projectId") String projectId);

    /**
     * 级联软删除关联数据
     */
    @Update("UPDATE ${table} SET is_deleted = 1, update_time = CURRENT_TIMESTAMP WHERE project_id = #{projectId} AND is_deleted = 0")
    void cascadeSoftDelete(@Param("table") String table, @Param("projectId") String projectId);

    /**
     * 软删除项目 (updateById 不更新 @TableLogic 字段，需自定义 SQL)
     */
    @Update("UPDATE project SET is_deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND is_deleted = 0")
    void softDeleteById(@Param("id") String id);
}
