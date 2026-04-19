package com.paperassistant.service.fulltext;

/**
 * 全文提取策略枚举
 *
 * @see PaperFullTextExtractor
 */
public enum ExtractorStrategy {

    /**
     * LaTeX 源码提取
     * <p>优点: 公式最完整 (保留 LaTeX 源码)、图表信息完整</p>
     * <p>缺点: 仅 arXiv 论文可用，需下载解压 .tar.gz</p>
     * <p>适用场景: 有源码的 arXiv 论文</p>
     * <p>成本: 低 (仅网络+解压)</p>
     */
    latex("LaTeX 源码提取", "仅 arXiv 论文可用"),

    /**
     * 视觉模型提取
     * <p>优点: 保留图表和公式的视觉呈现、支持任意 PDF</p>
     * <p>缺点: LLM 调用成本高、处理速度慢 (20 页论文约 5-10 秒)</p>
     * <p>适用场景: 无源码但有 PDF 的论文</p>
     * <p>成本: 高 (qwen2.5-vl token 消耗)</p>
     */
    vision("视觉模型提取", "LLM 调用成本高"),

    /**
     * ar5iv HTML 转换
     * <p>优点: 公式保留 MathJax 结构、文本质量好</p>
     * <p>缺点: 图表丢失、ar5iv 不支持所有 arXiv 论文</p>
     * <p>适用场景: arXiv 但无 LaTeX 源码</p>
     * <p>成本: 低 (HTTP 请求)</p>
     */
    ar5iv("ar5iv HTML 转换", "图表丢失"),

    /**
     * PDFBox 纯文本提取
     * <p>优点: 纯文本提取速度快、无需网络</p>
     * <p>缺点: 公式乱码、图表丢失、PDF 解析不稳定</p>
     * <p>适用场景: 任何 PDF 论文 (兜底)</p>
     * <p>成本: 低 (本地处理)</p>
     */
    pdfbox("PDFBox 纯文本提取", "公式乱码"),

    /**
     * 摘要回退
     * <p>优点: 始终可用、零成本</p>
     * <p>缺点: 信息严重不足、无法深度分析</p>
     * <p>适用场景: 所有全文提取均失败</p>
     * <p>成本: 零</p>
     */
    abstract_fallback("摘要回退", "信息严重不足");

    private final String description;
    private final String limitation;

    ExtractorStrategy(String description, String limitation) {
        this.description = description;
        this.limitation = limitation;
    }

    public String getDescription() {
        return description;
    }

    public String getLimitation() {
        return limitation;
    }
}
