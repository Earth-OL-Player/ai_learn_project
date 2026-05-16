export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

export const AUTH_TOKEN_STORAGE_KEY = 'ai_learn_access_token';

const SUCCESS_CODE = 'SUCCESS';
const REFRESH_TOKEN_HEADER = 'X-Refresh-Token';
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
 * 构造请求头，自动追加登录令牌。
 */
function buildHeaders(extraHeaders?: HeadersInit): HeadersInit {
  const headers = new Headers(extraHeaders);
  const token = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);

  // 登录后统一通过 Bearer token 访问受保护接口。
  headers.set('Accept', headers.get('Accept') || 'application/json');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  return headers;
}

/**
 * 保存后端自动续期返回的新令牌。
 */
function saveRefreshedToken(response: Response): void {
  const refreshedToken = response.headers.get(REFRESH_TOKEN_HEADER);
  if (refreshedToken) {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, refreshedToken);
  }
}

/**
 * 解析后端统一响应。
 */
async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error('服务暂时不可用，请稍后重试');
  }

  saveRefreshedToken(response);

  const result = (await response.json()) as ApiResponse<T>;
  if (result.code !== SUCCESS_CODE) {
    throw new Error(result.message || '请求处理失败');
  }
  return result.data;
}

/**
 * 发起 HTTP 请求并处理统一响应。
 */
async function request<T>(path: string, init: RequestInit): Promise<T> {
  try {
    const response = await fetch(buildRequestUrl(path), {
      ...init,
      headers: buildHeaders(init.headers),
    });
    return await parseResponse<T>(response);
  } catch (error) {
    if (error instanceof Error && error.message !== 'Failed to fetch') {
      throw error;
    }
    throw new Error('网络异常，请稍后重试');
  }
}

/**
 * 发起文件下载请求。
 */
async function requestBlob(path: string): Promise<Blob> {
  const response = await fetch(buildRequestUrl(path), {
    method: 'GET',
    headers: buildHeaders({ Accept: 'text/csv' }),
  });
  if (!response.ok) {
    throw new Error('文件下载失败，请稍后重试');
  }
  saveRefreshedToken(response);
  return response.blob();
}

/**
 * 发起 GET 请求。
 */
export async function get<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'GET' });
}

/**
 * 发起 POST 请求。
 */
export async function post<T, B = unknown>(path: string, body?: B): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

/**
 * 发起 PUT 请求。
 */
export async function put<T, B = unknown>(path: string, body?: B): Promise<T> {
  return request<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

/**
 * 发起表单 POST 请求。
 */
export async function postForm<T>(path: string, body: FormData): Promise<T> {
  return request<T>(path, { method: 'POST', body });
}

/**
 * 发起 DELETE 请求。
 */
export async function del<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'DELETE' });
}

/**
 * 发起下载请求。
 */
export async function getBlob(path: string): Promise<Blob> {
  return requestBlob(path);
}
