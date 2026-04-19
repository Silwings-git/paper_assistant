package com.paperassistant.exception;

/**
 * 业务异常码枚举
 */
public enum ErrorCode {

    // 参数错误 400x
    PARAM_INVALID(4001, "参数校验失败"),
    STATE_TRANSITION_INVALID(4002, "状态转换非法"),

    // 资源不存在 404x
    RESOURCE_NOT_FOUND(4041, "资源不存在"),

    // 内部错误 500x
    INTERNAL_ERROR(5001, "服务器内部错误"),

    // 外部 API 失败 502x
    EXTERNAL_API_FAILED(5021, "外部 API 调用失败"),
    ARXIV_API_FAILED(5022, "arXiv API 调用失败"),
    SEMANTIC_SCHOLAR_API_FAILED(5023, "Semantic Scholar API 调用失败"),

    // 限流 / LLM 调用失败 503x
    RATE_LIMITED(5031, "请求过于频繁，请稍后重试"),
    LLM_ERROR(5032, "LLM 调用失败"),
    LLM_RATE_LIMITED(5033, "LLM 请求被限流"),
    LLM_TIMEOUT(5034, "LLM 请求超时"),
    LLM_AUTH_ERROR(5035, "LLM 认证失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
