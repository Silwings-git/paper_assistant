package com.paperassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 项目响应 DTO
 */
@Schema(description = "项目信息")
public class ProjectDTO {

    @Schema(description = "项目ID", example = "proj-001")
    private String id;

    @Schema(description = "项目名称", example = "RAG 论文复现")
    private String name;

    @Schema(description = "项目描述", example = "使用 RAG 技术复现论文代码")
    private String description;

    @Schema(description = "研究方向", example = "检索增强生成、向量数据库、语义检索")
    private String topic;

    @Schema(description = "项目状态: CREATED(已创建)/SEARCHING(检索中)/SEARCHED(检索完成)/ANALYZING(分析中)/ANALYZED(分析完成)",
            example = "CREATED")
    private String status;

    @Schema(description = "基础论文ID", example = "paper-001")
    private String basePaperId;

    @Schema(description = "基础论文标题", example = "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks")
    private String basePaperTitle;

    @Schema(description = "论文数量", example = "10")
    private Integer paperCount;

    @Schema(description = "创建时间", example = "2026-04-19T12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-04-19T14:30:00")
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBasePaperId() {
        return basePaperId;
    }

    public void setBasePaperId(String basePaperId) {
        this.basePaperId = basePaperId;
    }

    public String getBasePaperTitle() {
        return basePaperTitle;
    }

    public void setBasePaperTitle(String basePaperTitle) {
        this.basePaperTitle = basePaperTitle;
    }

    public Integer getPaperCount() {
        return paperCount;
    }

    public void setPaperCount(Integer paperCount) {
        this.paperCount = paperCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
