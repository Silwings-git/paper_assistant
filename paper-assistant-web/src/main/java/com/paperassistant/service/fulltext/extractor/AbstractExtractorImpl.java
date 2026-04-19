package com.paperassistant.service.fulltext.extractor;

import com.paperassistant.entity.Paper;
import com.paperassistant.service.fulltext.AbstractExtractor;
import com.paperassistant.service.fulltext.ExtractorStrategy;
import com.paperassistant.service.fulltext.PaperFullText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 摘要回退提取器
 * <p>使用已有摘要填充 PaperFullText，最后兜底</p>
 * <p>优点: 始终可用、零成本</p>
 * <p>缺点: 信息严重不足、无法深度分析</p>
 *
 * @see ExtractorStrategy#abstract_fallback
 */
public class AbstractExtractorImpl extends AbstractExtractor {

    private static final Logger log = LoggerFactory.getLogger(AbstractExtractorImpl.class);

    @Override
    public String getStrategyName() {
        return ExtractorStrategy.abstract_fallback.name();
    }

    @Override
    protected int minTextLength() {
        return 50;
    }

    @Override
    protected PaperFullText doExtract(Paper paper) {
        if (paper.getAbstractText() == null || paper.getAbstractText().isEmpty()) {
            return null;
        }

        log.info("Falling back to abstract for paper: {}", paper.getArxivId());
        return new PaperFullText(
                paper.getAbstractText(),
                getStrategyName(),
                paper.getAbstractText().length(),
                "缺少全文，仅使用摘要");
    }
}
