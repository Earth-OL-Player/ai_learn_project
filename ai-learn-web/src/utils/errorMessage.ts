const DEFAULT_ERROR_MESSAGE = '操作失败，请稍后重试';

/**
 * 解析接口或运行时错误的展示文案。
 *
 * @param error 原始错误对象
 * @param fallback 无明确错误信息时的兜底文案
 * @return 可直接展示给用户的错误文案
 */
export function resolveErrorMessage(error: unknown, fallback = DEFAULT_ERROR_MESSAGE): string {
  // 只信任标准 Error 的 message，其它异常类型统一走页面提供的兜底文案。
  return error instanceof Error && error.message ? error.message : fallback;
}
