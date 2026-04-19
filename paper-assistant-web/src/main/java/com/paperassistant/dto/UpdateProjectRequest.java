package com.paperassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新项目请求 DTO
 */
@Schema(description = "更新项目请求体")
public class UpdateProjectRequest {

    @Schema(description = "项目名称", example = "RAG 论文复现 v2")
    private String name;

    @Schema(description = "项目描述", example = "优化后的 RAG 方案")
    private String description;

    @Schema(description = "研究方向", example = "混合检索、重排序、多路召回")
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
