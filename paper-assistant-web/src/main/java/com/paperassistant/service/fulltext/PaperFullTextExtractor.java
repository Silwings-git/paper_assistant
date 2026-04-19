package com.paperassistant.service.fulltext;

import com.paperassistant.entity.Paper;

/**
 * 论文全文提取器接口 (责任链模式)
 *
 * @see ExtractorStrategy
 */
public interface PaperFullTextExtractor {

    /**
     * 提取论文全文
     */
    PaperFullText extractFullText(Paper paper);

    /**
     * 获取当前策略名称
     */
    String getStrategyName();

    /**
     * 设置下一个提取器
     */
    void setNext(PaperFullTextExtractor next);
}
