package com.paperassistant.service.llm.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperassistant.exception.LlmException;
import com.paperassistant.service.llm.LlmProvider;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * DashScope LLM Provider (基于 LangChain4j)
 * 使用 OpenAI 兼容模式调用 DashScope API
 * base_url: https://dashscope.aliyuncs.com/compatible-mode/v1
 */
public class DashScopeProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(DashScopeProvider.class);

    private final ChatModel chatModel;
    private final String defaultModel;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    public DashScopeProvider(String apiKey, String defaultModel) {
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(apiKey)
                .modelName(defaultModel)
                .timeout(Duration.ofSeconds(300))
                .build();
        this.defaultModel = defaultModel;
        this.objectMapper = new ObjectMapper();
        log.info("DashScopeProvider initialized with model: {}, baseUrl: {} (OpenAI compatible mode)", defaultModel, BASE_URL);
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userPrompt)
                ))
                .build();

        try {
            ChatResponse response = chatModel.chat(request);
            String result = response.aiMessage().text();
            log.debug("DashScope chat completed, response length={}", result != null ? result.length() : 0);
            return result != null ? result : "";
        } catch (Exception e) {
            log.error("DashScope chat failed: {}", e.getMessage(), e);
            throw new LlmException("dashscope", "OTHER", "DashScope chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T chatStructured(String systemPrompt, String userPrompt, Class<T> responseSchema) {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userPrompt)
                ))
                .build();

        try {
            ChatResponse response = chatModel.chat(request);
            String json = response.aiMessage().text();
            if (json == null || json.isEmpty()) {
                throw new LlmException("dashscope", "OTHER", "Empty response from DashScope");
            }
            // 尝试提取 JSON（去除 markdown 代码块包裹）
            json = extractJson(json);
            T result = objectMapper.readValue(json, responseSchema);
            log.debug("DashScope structured chat completed, schema={}", responseSchema.getSimpleName());
            return result;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("DashScope structured chat failed: {}", e.getMessage(), e);
            throw new LlmException("dashscope", "OTHER",
                    "DashScope structured chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "dashscope";
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of("qwen3.6-plus", "qwen-plus", "qwen-max", "qwen-turbo");
    }

    /**
     * 从 LLM 响应中提取纯 JSON 字符串
     * 处理 markdown 代码块格式：```json ... ```
     */
    private String extractJson(String text) {
        text = text.trim();
        // 处理 ```json 包裹
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            if (start == -1) {
                return text;
            }
            int end = text.lastIndexOf("```");
            if (end > start) {
                text = text.substring(start + 1, end).trim();
            }
        }
        // 去除可能的 json 前缀
        if (text.startsWith("json")) {
            text = text.substring(4).trim();
        }
        return text;
    }
}
