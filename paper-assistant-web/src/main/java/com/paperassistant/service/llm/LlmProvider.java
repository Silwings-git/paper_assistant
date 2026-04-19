package com.paperassistant.service.llm;

import java.util.List;

/**
 * LLM Provider 接口，定义与 LLM 交互的统一契约
 *
 * <p>实现此接口即可接入新的 LLM 提供商，
 * 新增 provider 不改动业务代码，只需在数据库添加配置。</p>
 */
public interface LlmProvider {

    /**
     * 普通文本对话
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户消息
     * @return LLM 响应文本
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 结构化输出 (JSON → Java Object)
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户消息
     * @param responseSchema 期望的 Java 类型
     * @return 解析后的对象
     */
    <T> T chatStructured(String systemPrompt, String userPrompt, Class<T> responseSchema);

    /**
     * 提供者标识 (用于日志和路由)
     */
    String getProviderName();

    /**
     * 支持的模型列表
     */
    List<String> getSupportedModels();
}
