package com.paperassistant.service.fulltext.extractor;

import com.paperassistant.entity.Paper;
import com.paperassistant.service.fulltext.AbstractExtractor;
import com.paperassistant.service.fulltext.ExtractorStrategy;
import com.paperassistant.service.fulltext.PaperFullText;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * PDFBox 纯文本提取器
 * <p>PDFBox 提取纯文本，处理公式乱码情况</p>
 * <p>优点: 纯文本提取速度快、无需网络</p>
 * <p>缺点: 公式乱码、图表丢失、PDF 解析不稳定</p>
 *
 * @see ExtractorStrategy#pdfbox
 */
public class PdfBoxExtractor extends AbstractExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfBoxExtractor.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();

    @Override
    public String getStrategyName() {
        return ExtractorStrategy.pdfbox.name();
    }

    @Override
    protected int minTextLength() {
        return 500;
    }

    @Override
    protected PaperFullText doExtract(Paper paper) {
        if (paper.getPdfUrl() == null || paper.getPdfUrl().isEmpty()) {
            return null;
        }

        try {
            // Download PDF
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(paper.getPdfUrl()))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                return null;
            }

            // Extract text using PDFBox
            try (PDDocument document = Loader.loadPDF(response.body())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                if (text.length() < minTextLength()) {
                    return null;
                }

                return new PaperFullText(text, getStrategyName(), text.length(),
                        "公式可能乱码，图表信息丢失");
            }
        } catch (Exception e) {
            log.warn("PDFBox extraction failed for {}: {}", paper.getArxivId(), e.getMessage());
            return null;
        }
    }
}
