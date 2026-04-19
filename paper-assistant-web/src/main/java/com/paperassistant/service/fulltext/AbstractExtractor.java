package com.paperassistant.service.fulltext;

import com.paperassistant.entity.Paper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象提取器基类，实现责任链传递
 */
public abstract class AbstractExtractor implements PaperFullTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(AbstractExtractor.class);

    protected PaperFullTextExtractor next;

    @Override
    public void setNext(PaperFullTextExtractor next) {
        this.next = next;
    }

    @Override
    public PaperFullText extractFullText(Paper paper) {
        try {
            PaperFullText result = doExtract(paper);
            if (result != null && result.textLength() >= minTextLength()) {
                log.info("Extractor {} succeeded: {} chars", getStrategyName(), result.textLength());
                return result;
            }
        } catch (Exception e) {
            log.warn("Extractor {} failed: {}", getStrategyName(), e.getMessage());
        }

        // 责任链传递
        if (next != null) {
            return next.extractFullText(paper);
        }
        return PaperFullText.empty(getStrategyName());
    }

    /**
     * 具体提取逻辑
     */
    protected abstract PaperFullText doExtract(Paper paper);

    /**
     * 最小有效文本长度
     */
    protected abstract int minTextLength();
}
