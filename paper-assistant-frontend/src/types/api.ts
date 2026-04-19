export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export enum ErrorCode {
  PARAM_INVALID = 4001,
  RESOURCE_NOT_FOUND = 4041,
  EXTERNAL_API_FAILED = 5021,
  RATE_LIMITED = 5031,
  LLM_ERROR = 5032,
  INTERNAL_ERROR = 5001,
  STATE_TRANSITION_INVALID = 4002,
}
