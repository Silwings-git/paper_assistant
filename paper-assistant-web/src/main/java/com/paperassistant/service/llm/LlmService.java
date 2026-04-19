package com.paperassistant.service.llm;

import com.paperassistant.exception.LlmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * LLM 服务：场景路由 + 统一超时 + 指数退避重试
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);
    private static final int MAX_RETRIES = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(300);

    private final LlmProviderFactory providerFactory;

    // 场景 → 模型映射 (后续从数据库加载)
    private static final Map<String, String> SCENE_MODEL_MAP = Map.of(
            "analysis", "qwen-plus",
            "writing", "qwen-plus",
            "general", "qwen-plus"
    );

    public LlmService(LlmProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    /**
     * 场景路由调用 LLM
     *
     * @param scene        场景: analysis/writing/general
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户消息
     * @return LLM 响应
     */
    public String chat(String scene, String systemPrompt, String userPrompt) {
        LlmProvider provider = resolveProvider(scene);
        return retry(() -> provider.chat(systemPrompt, userPrompt), provider.getProviderName());
    }

    /**
     * 场景路由结构化调用
     */
    public <T> T chatStructured(String scene, String systemPrompt, String userPrompt, Class<T> responseSchema) {
        LlmProvider provider = resolveProvider(scene);
        return retry(() -> provider.chatStructured(systemPrompt, userPrompt, responseSchema), provider.getProviderName());
    }

    /**
     * 根据场景解析 Provider
     */
    private LlmProvider resolveProvider(String scene) {
        String model = SCENE_MODEL_MAP.getOrDefault(scene, "general");
        String providerType = "dashscope"; // MVP: 仅 DashScope，后续扩展多 provider
        LlmProvider provider = providerFactory.getProvider(providerType);
        log.debug("Resolved scene={} to provider={}, model={}", scene, providerType, model);
        return provider;
    }

    private <T> T retry(Supplier<T> call, String providerName) {
        long delayMs = 1000;
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return call.get();
            } catch (LlmException e) {
                lastException = e;
                if (!e.isRetryable()) {
                    log.error("Non-retryable LLM error from {}: {}", providerName, e.getMessage());
                    throw e;
                }
                log.warn("LLM call failed (attempt {}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
            } catch (Exception e) {
                lastException = e;
                log.warn("Unexpected error from LLM call (attempt {}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
            }

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new LlmException(providerName, "OTHER", "Retry interrupted", ie);
            }
            delayMs *= 2;
        }

        throw new LlmException(providerName, "OTHER",
                "LLM call failed after " + MAX_RETRIES + " retries", lastException);
    }
}
