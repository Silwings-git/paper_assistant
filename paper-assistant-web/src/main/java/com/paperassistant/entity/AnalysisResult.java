package com.paperassistant.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.type.JdbcType;

/**
 * 分析结果实体
 */
@Schema(description = "分析结果 (数据库实体，以JSON字符串形式存储)")
@TableName("analysis_result")
public class AnalysisResult extends BaseEntity {

    @Schema(description = "所属项目ID", example = "1")
    private String projectId;

    @Schema(description = "研究差距 JSON字符串")
    @TableField(value = "gaps", jdbcType = JdbcType.OTHER)
    private String gaps;      // JSONB

    @Schema(description = "基础论文推荐 JSON字符串")
    @TableField(value = "base_papers", jdbcType = JdbcType.OTHER)
    private String basePapers; // JSONB

    @Schema(description = "创新点 JSON字符串")
    @TableField(value = "innovation_pts", jdbcType = JdbcType.OTHER)
    private String innovationPts; // JSONB

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getGaps() {
        return gaps;
    }

    public void setGaps(String gaps) {
        this.gaps = gaps;
    }

    public String getBasePapers() {
        return basePapers;
    }

    public void setBasePapers(String basePapers) {
        this.basePapers = basePapers;
    }

    public String getInnovationPts() {
        return innovationPts;
    }

    public void setInnovationPts(String innovationPts) {
        this.innovationPts = innovationPts;
    }
}
