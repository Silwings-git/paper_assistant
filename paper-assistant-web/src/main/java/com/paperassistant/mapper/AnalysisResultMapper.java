package com.paperassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paperassistant.entity.AnalysisResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnalysisResultMapper extends BaseMapper<AnalysisResult> {

    @Select("SELECT * FROM analysis_result WHERE project_id = #{projectId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT 1")
    AnalysisResult selectLatestByProjectId(String projectId);

    /**
     * 插入分析结果，JSONB 列显式转换
     */
    @Insert("""
            INSERT INTO analysis_result (id, project_id, gaps, base_papers, innovation_pts, create_time, update_time, is_deleted)
            VALUES (#{id}, #{projectId}, #{gaps}::jsonb, #{basePapers}::jsonb, #{innovationPts}::jsonb, NOW(), NOW(), 0)
            """)
    int insertWithJsonB(@Param("id") String id,
                        @Param("projectId") String projectId,
                        @Param("gaps") String gaps,
                        @Param("basePapers") String basePapers,
                        @Param("innovationPts") String innovationPts);
}
