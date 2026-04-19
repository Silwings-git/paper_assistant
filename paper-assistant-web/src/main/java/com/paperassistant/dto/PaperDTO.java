package com.paperassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 论文响应 DTO
 */
@Schema(description = "论文信息")
public class PaperDTO {

    @Schema(description = "论文ID", example = "paper-001")
    private String id;

    @Schema(description = "所属项目ID", example = "proj-001")
    private String projectId;

    @Schema(description = "论文来源", example = "arxiv")
    private String source;

    @Schema(description = "来源论文ID", example = "2401.12345")
    private String sourceId;

    @Schema(description = "arXiv 论文ID", example = "2401.12345")
    private String arxivId;

    @Schema(description = "论文标题", example = "Attention Is All You Need")
    private String title;

    @Schema(description = "论文摘要")
    private String abstractText;

    @Schema(description = "作者列表", example = "[\"Vaswani, A.\", \"Shazeer, N.\", \"Parmar, N.\"]")
    private List<String> authors;

    @Schema(description = "发表日期", example = "2023-06-15")
    private LocalDate publishDate;

    @Schema(description = "引用次数", example = "50000")
    private Integer citationCount;

    @Schema(description = "影响力评分", example = "0.95")
    private Double influenceScore;

    @Schema(description = "是否有开源代码", example = "true")
    private Boolean hasCode;

    @Schema(description = "代码仓库URL", example = "https://github.com/example/repo")
    private String codeUrl;

    @Schema(description = "PDF下载URL", example = "https://arxiv.org/pdf/2401.12345")
    private String pdfUrl;

    @Schema(description = "论文分类", example = "cs.CL")
    private String category;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getArxivId() {
        return arxivId;
    }

    public void setArxivId(String arxivId) {
        this.arxivId = arxivId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDate publishDate) {
        this.publishDate = publishDate;
    }

    public Integer getCitationCount() {
        return citationCount;
    }

    public void setCitationCount(Integer citationCount) {
        this.citationCount = citationCount;
    }

    public Double getInfluenceScore() {
        return influenceScore;
    }

    public void setInfluenceScore(Double influenceScore) {
        this.influenceScore = influenceScore;
    }

    public Boolean getHasCode() {
        return hasCode;
    }

    public void setHasCode(Boolean hasCode) {
        this.hasCode = hasCode;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public void setCodeUrl(String codeUrl) {
        this.codeUrl = codeUrl;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
