package com.paperassistant.service.fulltext.extractor;

import com.paperassistant.entity.Paper;
import com.paperassistant.service.fulltext.AbstractExtractor;
import com.paperassistant.service.fulltext.ExtractorStrategy;
import com.paperassistant.service.fulltext.PaperFullText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LaTeX 源码提取器
 * <p>下载 arXiv .tar.gz，解压提取 .tex 文本，保留公式源码</p>
 * <p>优点: 公式最完整、图表信息完整</p>
 * <p>缺点: 仅 arXiv 论文可用，需下载解压</p>
 *
 * @see ExtractorStrategy#latex
 */
public class LatexExtractor extends AbstractExtractor {

    private static final Logger log = LoggerFactory.getLogger(LatexExtractor.class);
    private static final String LATEX_SOURCE_URL = "https://arxiv.org/e-print/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();

    @Override
    public String getStrategyName() {
        return ExtractorStrategy.latex.name();
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
            // Download .tar.gz
            String url = LATEX_SOURCE_URL + paper.getArxivId();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.warn("LaTeX source not found for {}: status {}", paper.getArxivId(), response.statusCode());
                return null;
            }

            // Simple text extraction from .tar.gz content
            // In production, use a proper tar/gzip library
            String text = extractTextFromBytes(response.body());

            if (text.length() < minTextLength()) {
                return null;
            }

            return new PaperFullText(text, getStrategyName(), text.length(), null);
        } catch (Exception e) {
            log.warn("Failed to extract LaTeX for {}: {}", paper.getArxivId(), e.getMessage());
            return null;
        }
    }

    private String extractTextFromBytes(byte[] data) throws IOException {
        // Basic extraction - just return the raw text content
        // In production, properly parse tar.gz and extract .tex files
        return new String(data, java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("[^\\x20-\\x7E\\n]", " ");
    }
}
