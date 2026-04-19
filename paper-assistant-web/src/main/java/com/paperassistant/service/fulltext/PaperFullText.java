package com.paperassistant.service.fulltext;

/**
 * 论文全文提取结果
 */
public record PaperFullText(
        String fullText,
        String strategy,
        int textLength,
        String warnings
) {
    public static PaperFullText empty(String strategy) {
        return new PaperFullText("", strategy, 0, "");
    }
}
