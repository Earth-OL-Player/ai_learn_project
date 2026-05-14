interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

const SUCCESS_CODE = 'SUCCESS';
const DEFAULT_API_BASE_URL = 'http://localhost:8080/api/v1';

/**
 * 获取接口基础地址。
 */
function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL;
}

/**
 * 拼接完整请求地址。
 */
function buildRequestUrl(path: string): string {
  const baseUrl = getApiBaseUrl().replace(/\/$/, '');
  const requestPath = path.startsWith('/') ? path : `/${path}`;
  return `${baseUrl}${requestPath}`;
}

/**
 * 发起 GET 请求并处理统一响应。
 */
export async function get<T>(path: string): Promise<T> {
  try {
    const response = await fetch(buildRequestUrl(path), {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    });

    // 后端非 2xx 时给出统一中文提示。
    if (!response.ok) {
      throw new Error('服务暂时不可用，请稍后重试');
    }

    const result = (await response.json()) as ApiResponse<T>;
    if (result.code !== SUCCESS_CODE) {
      throw new Error(result.message || '请求处理失败');
    }

    return result.data;
  } catch (error) {
    if (error instanceof Error && error.message !== 'Failed to fetch') {
      throw error;
    }
    throw new Error('网络异常，请稍后重试');
  }
}
