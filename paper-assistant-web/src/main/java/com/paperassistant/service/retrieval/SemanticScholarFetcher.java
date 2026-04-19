package com.paperassistant.service.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperassistant.exception.BusinessException;
import com.paperassistant.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Semantic Scholar API 论文获取器
 * 补充 citationCount / influenceScore / hasCode / codeUrl
 * 5分钟≤100次，单源上限 100 条
 */
@Service
public class SemanticScholarFetcher {

    private static final Logger log = LoggerFactory.getLogger(SemanticScholarFetcher.class);
    private static final String API_URL = "https://api.semanticscholar.org/graph/v1/paper/search";
    private static final int MAX_RESULTS = 100;
    // 滑动窗口限流：5 分钟最多 100 次请求
    private static final int RATE_LIMIT_MAX = 100;
    private static final long RATE_LIMIT_WINDOW_MS = 5 * 60 * 1000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 滑动窗口：记录每次请求的时间戳
    private final ConcurrentLinkedQueue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();
    private final ReentrantLock rateLimitLock = new ReentrantLock();

    /**
     * 滑动窗口限流检查
     * 5 分钟内最多 RATE_LIMIT_MAX 次请求
     */
    private void checkRateLimit() {
        rateLimitLock.lock();
        try {
            long now = System.currentTimeMillis();
            long windowStart = now - RATE_LIMIT_WINDOW_MS;

            // 清除窗口外的时间戳
            requestTimestamps.removeIf(ts -> ts < windowStart);

            if (requestTimestamps.size() >= RATE_LIMIT_MAX) {
                log.warn("Semantic Scholar API rate limit reached ({} requests in 5 min)", RATE_LIMIT_MAX);
                throw new BusinessException(ErrorCode.RATE_LIMITED,
                        "Semantic Scholar API 限流：5 分钟内已达 " + RATE_LIMIT_MAX + " 次请求上限");
            }

            requestTimestamps.add(now);
        } finally {
            rateLimitLock.unlock();
        }
    }

    /**
     * 根据关键词检索 Semantic Scholar 论文
     */
    public List<Map<String, Object>> fetch(String keyword) {
        // 限流检查
        checkRateLimit();

        String url = API_URL + "?query=" + encode(keyword)
                + "&limit=" + MAX_RESULTS
                + "&fields=title,abstract,authors,year,citationCount,externalIds,url,s2FieldsOfStudy";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                log.warn("Semantic Scholar API rate limited");
                throw new BusinessException(ErrorCode.RATE_LIMITED, "Semantic Scholar API 被限流");
            }

            if (response.statusCode() != 200) {
                log.warn("Semantic Scholar API returned status {}", response.statusCode());
                throw new BusinessException(ErrorCode.SEMANTIC_SCHOLAR_API_FAILED,
                        "Semantic Scholar API 返回错误: " + response.statusCode());
            }

            return parseResponse(response.body());
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Semantic Scholar fetch interrupted", e);
            return List.of();
        } catch (Exception e) {
            log.error("Semantic Scholar API request failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SEMANTIC_SCHOLAR_API_FAILED,
                    "Semantic Scholar API 请求失败: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> parseResponse(String body) throws Exception {
        List<Map<String, Object>> papers = new ArrayList<>();
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.get("data");

        if (data == null || !data.isArray()) {
            return papers;
        }

        for (JsonNode paper : data) {
            papers.add(parsePaper(paper));
        }

        log.info("Semantic Scholar fetch completed, got {} papers", papers.size());
        return papers;
    }

    private Map<String, Object> parsePaper(JsonNode paper) {
        String title = paper.has("title") && !paper.get("title").isNull() ? paper.get("title").asText() : "";
        String abstractText = paper.has("abstract") && !paper.get("abstract").isNull() ? paper.get("abstract").asText() : "";
        int citationCount = paper.has("citationCount") && !paper.get("citationCount").isNull() ? paper.get("citationCount").asInt() : 0;

        // 提取作者
        List<String> authors = new ArrayList<>();
        JsonNode authorsNode = paper.get("authors");
        if (authorsNode != null && authorsNode.isArray()) {
            for (JsonNode author : authorsNode) {
                if (author.has("name")) {
                    authors.add(author.get("name").asText());
                }
            }
        }

        // 提取 arXiv ID 和 Semantic Scholar ID
        String arxivId = "";
        String s2Id = "";
        JsonNode externalIds = paper.get("externalIds");
        if (externalIds != null && externalIds.has("ArXiv")) {
            arxivId = externalIds.get("ArXiv").asText();
        }
        if (externalIds != null && externalIds.has("SemanticScholar")) {
            s2Id = externalIds.get("SemanticScholar").asText();
        }
        // 如果 s2Id 为空，使用 url 中的 ID 作为后备
        if (s2Id.isEmpty()) {
            String url = paper.has("url") && !paper.get("url").isNull() ? paper.get("url").asText() : "";
            if (!url.isEmpty()) {
                String[] parts = url.split("/");
                s2Id = parts[parts.length - 1];
            }
        }

        // 提取代码 URL
        String codeUrl = "";
        boolean hasCode = false;
        if (externalIds != null && externalIds.has("GitHub")) {
            codeUrl = externalIds.get("GitHub").asText();
            hasCode = true;
        }

        int year = paper.has("year") && !paper.get("year").isNull() ? paper.get("year").asInt() : 0;

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("title", title);
        result.put("abstract", abstractText);
        result.put("authors", authors);
        result.put("publishDate", year > 0 ? java.time.LocalDate.ofYearDay(year, 1) : null);
        result.put("arxivId", arxivId);
        result.put("source", "semanticscholar");
        result.put("sourceId", s2Id);
        result.put("pdfUrl", "");
        result.put("category", "");
        result.put("citationCount", citationCount);
        result.put("influenceScore", (double) citationCount);
        result.put("hasCode", hasCode);
        result.put("codeUrl", codeUrl);
        return result;
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
