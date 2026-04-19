package com.paperassistant.exception;

/**
 * LLM 调用统一异常
 */
public class LlmException extends RuntimeException {

    private final String providerName;
    private final String errorType; // RATE_LIMIT / TIMEOUT / AUTH / OTHER

    public LlmException(String providerName, String errorType, String message) {
        super(message);
        this.providerName = providerName;
        this.errorType = errorType;
    }

    public LlmException(String providerName, String errorType, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.errorType = errorType;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getErrorType() {
        return errorType;
    }

    public boolean isRetryable() {
        return !"AUTH".equals(errorType);
    }
}
