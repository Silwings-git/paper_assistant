package com.paperassistant.entity;

/**
 * 项目状态枚举
 */
public enum ProjectStatus {

    CREATED("已创建"),
    SEARCHING("检索中"),
    SEARCHED("检索完成"),
    ANALYZING("分析中"),
    ANALYZED("分析完成");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ProjectStatus fromString(String value) {
        for (ProjectStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown project status: " + value);
    }
}
