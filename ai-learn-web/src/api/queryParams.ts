export type QueryParamValue = string | number | boolean | null | undefined;

export type QueryParams = Record<string, QueryParamValue>;

/**
 * 构造带查询参数的接口路径。
 */
export function buildQueryPath(path: string, params: QueryParams): string {
  const queryString = buildQueryString(params);
  return queryString ? `${path}?${queryString}` : path;
}

/**
 * 构造 URL 查询字符串。
 */
function buildQueryString(params: QueryParams): string {
  const queryParams = new URLSearchParams();

  // 统一处理空字符串、空值和数字分页参数，避免各 API 文件重复拼接。
  Object.entries(params).forEach(([key, value]) => appendQueryParam(queryParams, key, value));
  return queryParams.toString();
}

/**
 * 追加单个查询参数。
 */
function appendQueryParam(params: URLSearchParams, key: string, value: QueryParamValue): void {
  if (value === null || value === undefined) {
    return;
  }
  if (typeof value === 'string') {
    appendStringQueryParam(params, key, value);
    return;
  }

  // 非字符串参数主要用于分页和布尔筛选，直接转为标准查询值。
  params.set(key, String(value));
}

/**
 * 追加字符串查询参数。
 */
function appendStringQueryParam(params: URLSearchParams, key: string, value: string): void {
  const safeValue = value.trim();
  if (!safeValue) {
    return;
  }
  params.set(key, safeValue);
}
