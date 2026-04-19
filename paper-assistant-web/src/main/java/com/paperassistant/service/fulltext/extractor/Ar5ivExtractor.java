package com.paperassistant.service.fulltext.extractor;

import com.paperassistant.entity.Paper;
import com.paperassistant.service.fulltext.AbstractExtractor;
import com.paperassistant.service.fulltext.ExtractorStrategy;
import com.paperassistant.service.fulltext.PaperFullText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ar5iv HTML 提取器
 * <p>访问 ar5iv HTML 版本，提取文本，公式转 MathJax</p>
 * <p>优点: 公式保留 MathJax 结构、文本质量好</p>
 * <p>缺点: 图表丢失、ar5iv 不支持所有 arXiv 论文</p>
 *
 * @see ExtractorStrategy#ar5iv
 */
public class Ar5ivExtractor extends AbstractExtractor {

    private static final Logger log = LoggerFactory.getLogger(Ar5ivExtractor.class);
    private static final String AR5IV_URL = "https://ar5iv.labs.arxiv.org/html/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();

    @Override
    public String getStrategyName() {
        return ExtractorStrategy.ar5iv.name();
    }

    @Override
    protected int minTextLength() {
        return 500;
    }

    @Override
    protected PaperFullText doExtract(Paper paper) {
        if (paper.getArxivId() == null || paper.getArxivId().isEmpty()) {
            return null;
        }

        try {
            String url = AR5IV_URL + paper.getArxivId();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            // Strip HTML tags to get text content
            String text = response.body().replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (text.length() < minTextLength()) {
                return null;
            }

            return new PaperFullText(text, getStrategyName(), text.length(),
                    "图表信息丢失");
        } catch (Exception e) {
            log.warn("ar5iv extraction failed for {}: {}", paper.getArxivId(), e.getMessage());
            return null;
        }
    }
}
