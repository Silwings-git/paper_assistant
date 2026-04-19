package com.paperassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建项目请求 DTO
 */
@Schema(description = "创建项目请求体")
public class CreateProjectRequest {

    @Schema(description = "项目名称", example = "RAG 论文复现")
    @NotBlank(message = "项目名称不能为空")
    private String name;

    @Schema(description = "项目描述", example = "使用 RAG 技术复现论文代码")
    private String description;

    @Schema(description = "研究方向", example = "检索增强生成、向量数据库、语义检索")
    @NotBlank(message = "研究方向不能为空")
    @Size(max = 500, message = "研究方向长度不能超过500字符")
    private String topic;

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
}
