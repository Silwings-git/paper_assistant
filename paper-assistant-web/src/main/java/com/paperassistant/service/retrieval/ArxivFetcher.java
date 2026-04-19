package com.paperassistant.service.retrieval;

import com.paperassistant.exception.BusinessException;
import com.paperassistant.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * arXiv API 论文获取器
 * 调用 arXiv API 解析 Atom XML，提取论文元数据
 * 请求间隔 ≥3s，单源上限 100 条
 */
@Service
public class ArxivFetcher {

    private static final Logger log = LoggerFactory.getLogger(ArxivFetcher.class);
    private static final String ARXIV_API_URL = "https://export.arxiv.org/api/query";
    private static final int MAX_RESULTS = 100;
    private static final int PAGE_SIZE = 10;
    private static final long REQUEST_INTERVAL_MS = 3000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();

    /**
     * 根据关键词检索 arXiv 论文
     *
     * @param keyword 搜索关键词
     * @return 论文列表
     */
    public List<Map<String, Object>> fetch(String keyword) {
        List<Map<String, Object>> papers = new ArrayList<>();
        int start = 0;
        int totalFetched = 0;

        while (totalFetched < MAX_RESULTS) {
            int maxResults = Math.min(PAGE_SIZE, MAX_RESULTS - totalFetched);
            String url = ARXIV_API_URL + "?search_query=all:" + encode(keyword)
                    + "&start=" + start + "&max_results=" + maxResults
                    + "&sortBy=relevance&sortOrder=descending";

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.warn("arXiv API returned status {}", response.statusCode());
                    break;
                }

                List<Map<String, Object>> page = parseAtomXml(response.body());
                if (page.isEmpty()) {
                    break;
                }

                papers.addAll(page);
                totalFetched += page.size();
                start += PAGE_SIZE;

                if (page.size() < PAGE_SIZE) {
                    break;
                }

                // 请求间隔 ≥3s
                Thread.sleep(REQUEST_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("arXiv fetch interrupted", e);
                break;
            } catch (Exception e) {
                log.error("arXiv API request failed: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.ARXIV_API_FAILED, "arXiv API 请求失败: " + e.getMessage(), e);
            }
        }

        log.info("arXiv fetch completed for '{}', got {} papers", keyword, papers.size());
        return papers;
    }

    private List<Map<String, Object>> parseAtomXml(String xml) throws Exception {
        List<Map<String, Object>> papers = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        NodeList entries = doc.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            papers.add(parseEntry(entry));
        }

        return papers;
    }

    private Map<String, Object> parseEntry(Element entry) {
        String title = getTextContent(entry, "title").replaceAll("\\s+", " ").trim();
        String abstractText = getTextContent(entry, "summary").trim();
        String published = getTextContent(entry, "published");
        String pdfUrl = "";

        // 提取 PDF 链接
        NodeList links = entry.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            if ("pdf".equals(link.getAttribute("title"))) {
                pdfUrl = link.getAttribute("href");
                break;
            }
        }

        // 提取作者
        List<String> authors = new ArrayList<>();
        NodeList authorElements = entry.getElementsByTagName("author");
        for (int i = 0; i < authorElements.getLength(); i++) {
            Element author = (Element) authorElements.item(i);
            String name = getTextContent(author, "name");
            if (!name.isEmpty()) {
                authors.add(name);
            }
        }

        // 提取分类
        String category = "";
        NodeList categories = entry.getElementsByTagName("category");
        if (categories.getLength() > 0) {
            category = ((Element) categories.item(0)).getAttribute("term");
        }

        // 提取 arXiv ID
        String id = getTextContent(entry, "id");
        String arxivId = id.replaceAll(".*abs/", "");

        LocalDate publishDate = null;
        if (!published.isEmpty()) {
            try {
                publishDate = LocalDate.parse(published.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                log.warn("Failed to parse date: {}", published);
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("title", title);
        result.put("abstract", abstractText);
        result.put("authors", authors);
        result.put("publishDate", publishDate);
        result.put("arxivId", arxivId);
        result.put("source", "arxiv");
        result.put("sourceId", arxivId);
        result.put("pdfUrl", pdfUrl);
        result.put("category", category);
        result.put("citationCount", 0);
        result.put("influenceScore", 0.0);
        result.put("hasCode", false);
        result.put("codeUrl", "");
        return result;
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return "";
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
