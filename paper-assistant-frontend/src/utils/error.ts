/**
 * 从 AxiosError 或其他错误对象中提取可读的错误消息
 */
export function extractErrorMessage(e: unknown): string {
  const err = e as Record<string, unknown>
  // AxiosError: 优先取后端返回的业务消息
  if (err.response && typeof err.response === 'object') {
    const data = (err.response as Record<string, unknown>).data
    if (data && typeof data === 'object') {
      const msg = (data as Record<string, unknown>).message
      if (typeof msg === 'string' && msg) return msg
    }
  }
  // AxiosError 的 message 字段
  if (typeof err.message === 'string' && err.message) return err.message
  // 兜底
  return '请求失败'
}
