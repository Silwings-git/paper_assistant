package com.paperassistant.service.fulltext;

import com.paperassistant.service.fulltext.extractor.AbstractExtractorImpl;
import com.paperassistant.service.fulltext.extractor.Ar5ivExtractor;
import com.paperassistant.service.fulltext.extractor.LatexExtractor;
import com.paperassistant.service.fulltext.extractor.PdfBoxExtractor;
import com.paperassistant.service.fulltext.extractor.VisionExtractor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 全文提取器配置，从 application.yml 读取 extractor-order 列表组装责任链
 */
@Configuration
@ConfigurationProperties(prefix = "paper.fulltext")
public class PaperFullTextExtractorConfig {

    private List<String> extractorOrder;

    @Bean
    public PaperFullTextExtractor paperFullTextExtractor() {
        List<String> order = extractorOrder != null ? extractorOrder :
                List.of("latex", "vision", "ar5iv", "pdfbox", "abstract");

        PaperFullTextExtractor head = null;
        PaperFullTextExtractor current = null;

        for (String name : order) {
            PaperFullTextExtractor extractor = createExtractor(name);
            if (extractor == null) continue;

            if (head == null) {
                head = extractor;
                current = extractor;
            } else {
                current.setNext(extractor);
                current = extractor;
            }
        }

        return head;
    }

    private PaperFullTextExtractor createExtractor(String name) {
        return switch (name) {
            case "latex" -> new LatexExtractor();
            case "vision" -> new VisionExtractor();
            case "ar5iv" -> new Ar5ivExtractor();
            case "pdfbox" -> new PdfBoxExtractor();
            case "abstract" -> new AbstractExtractorImpl();
            default -> throw new IllegalArgumentException("Unknown extractor: " + name);
        };
    }

    public List<String> getExtractorOrder() {
        return extractorOrder;
    }

    public void setExtractorOrder(List<String> extractorOrder) {
        this.extractorOrder = extractorOrder;
    }
}
