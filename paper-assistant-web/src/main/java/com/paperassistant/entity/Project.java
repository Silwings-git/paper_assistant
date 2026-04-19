package com.paperassistant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 项目实体
 */
@TableName("project")
public class Project extends BaseEntity {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "研究方向不能为空")
    @Size(max = 500, message = "研究方向长度不能超过500字符")
    private String topic;

    /**
     * @see ProjectStatus
     */
    private String status;

    private String basePaperId;

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
}
