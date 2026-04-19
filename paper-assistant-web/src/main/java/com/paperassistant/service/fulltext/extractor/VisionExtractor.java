package com.paperassistant.service.fulltext.extractor;

import com.paperassistant.entity.Paper;
import com.paperassistant.service.fulltext.AbstractExtractor;
import com.paperassistant.service.fulltext.ExtractorStrategy;
import com.paperassistant.service.fulltext.PaperFullText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 视觉模型提取器
 * <p>PDF→PNG 300DPI，调用 qwen2.5-vl 视觉模型提取</p>
 * <p>优点: 保留图表和公式的视觉呈现、支持任意 PDF</p>
 * <p>缺点: LLM 调用成本高、处理速度慢</p>
 *
 * @see ExtractorStrategy#vision
 */
public class VisionExtractor extends AbstractExtractor {

    private static final Logger log = LoggerFactory.getLogger(VisionExtractor.class);

    @Override
    public String getStrategyName() {
        return ExtractorStrategy.vision.name();
    }

    @Override
    protected int minTextLength() {
        return 1000; // At least 3 pages worth of text
    }

    @Override
    protected PaperFullText doExtract(Paper paper) {
        // TODO: Implement PDF to PNG conversion and qwen2.5-vl call
        log.info("Vision extraction not yet implemented for paper: {}", paper.getArxivId());
        return null;
    }
}
