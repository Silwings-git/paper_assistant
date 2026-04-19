package com.paperassistant.service.llm;

import com.paperassistant.exception.BusinessException;
import com.paperassistant.exception.ErrorCode;
import com.paperassistant.service.llm.provider.DashScopeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * LLM Provider 工厂，根据 provider_type 动态返回对应实例
 */
@Component
public class LlmProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderFactory.class);

    @Value("${llm.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${llm.dashscope.default-model:qwen-plus}")
    private String dashscopeDefaultModel;

    private final Map<String, LlmProvider> providers = new HashMap<>();

    @PostConstruct
    public void init() {
        if (dashscopeApiKey != null && !dashscopeApiKey.isEmpty()) {
            DashScopeProvider provider = new DashScopeProvider(dashscopeApiKey, dashscopeDefaultModel);
            providers.put("dashscope", provider);
            log.info("Registered LLM provider: dashscope ({})", dashscopeDefaultModel);
        } else {
            log.warn("No DashScope API key configured, LLM features will not work");
        }
    }

    /**
     * 根据 provider_type 获取 Provider 实例
     */
    public LlmProvider getProvider(String providerType) {
        LlmProvider provider = providers.get(providerType.toLowerCase());
        if (provider == null) {
            throw new BusinessException(ErrorCode.LLM_ERROR, "未找到 LLM provider: " + providerType);
        }
        return provider;
    }

    /**
     * 获取默认 Provider
     */
    public LlmProvider getDefaultProvider() {
        return getProvider("dashscope");
    }
}
