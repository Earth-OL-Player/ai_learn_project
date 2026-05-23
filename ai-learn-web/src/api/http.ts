import { API_SUCCESS_CODE, DEFAULT_API_BASE_URL, REFRESH_TOKEN_HEADER } from '../constants/api';
import { clearStoredAccessToken, getStoredAccessToken, setStoredAccessToken } from '../utils/authTokenStorage';

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

export interface StreamEvent {
  event: string;
  data: string;
}

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
function buildHeaders(extraHeaders?: HeadersInit, includeAuthToken = true): HeadersInit {
  const headers = new Headers(extraHeaders);
  const token = getStoredAccessToken();

  // 登录后统一通过 Bearer token 访问受保护接口。
  headers.set('Accept', headers.get('Accept') || 'application/json');
  if (includeAuthToken && token) {
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
    setStoredAccessToken(refreshedToken);
  }
}

/**
 * 解析非 2xx 响应中的错误消息。
 */
async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const result = (await response.json()) as ApiResponse<unknown>;
    return result.message || '请求处理失败';
  } catch {
    return response.status === 401 ? '登录状态已失效，请重新登录' : '服务暂时不可用，请稍后重试';
  }
}

/**
 * 解析后端统一响应。
 */
async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const message = await parseErrorMessage(response);
    if (response.status === 401) {
      clearStoredAccessToken();
    }
    throw new Error(message);
  }

  saveRefreshedToken(response);

  const result = (await response.json()) as ApiResponse<T>;
  if (result.code !== API_SUCCESS_CODE) {
    throw new Error(result.message || '请求处理失败');
  }
  return result.data;
}

/**
 * 发起 HTTP 请求并处理统一响应。
 */
async function request<T>(path: string, init: RequestInit, includeAuthToken = true): Promise<T> {
  try {
    const response = await fetch(buildRequestUrl(path), {
      ...init,
      headers: buildHeaders(init.headers, includeAuthToken),
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
    const message = await parseErrorMessage(response);
    if (response.status === 401) {
      clearStoredAccessToken();
    }
    throw new Error(message || '文件下载失败，请稍后重试');
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
 * 发起游客公开 GET 请求，不携带本地登录令牌。
 */
export async function getPublic<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'GET' }, false);
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
 * 发起 POST 流式请求。
 */
export async function postStream<B = unknown>(
  path: string,
  body: B,
  onEvent: (event: StreamEvent) => void,
): Promise<void> {
  const response = await fetch(buildRequestUrl(path), {
    method: 'POST',
    headers: buildHeaders({
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
    }),
    body: JSON.stringify(body),
  });

  // 流式接口仍复用后端自动续期 token。
  if (!response.ok || !response.body) {
    const message = await parseErrorMessage(response);
    if (response.status === 401) {
      clearStoredAccessToken();
    }
    throw new Error(message);
  }
  saveRefreshedToken(response);
  await readEventStream(response.body, onEvent);
}

/**
 * 读取 SSE 响应流。
 */
async function readEventStream(stream: ReadableStream<Uint8Array>, onEvent: (event: StreamEvent) => void): Promise<void> {
  const reader = stream.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let readCount = 0;
  const startTime = performance.now();

  // 持续解析服务端推送的事件块，直到流结束。
  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    readCount += 1;
    logStreamReadChunk(readCount, value.byteLength, startTime);
    buffer += decoder.decode(value, { stream: true });
    buffer = dispatchBufferedEvents(buffer, onEvent);
  }

  // 处理流结束后残留的最后一个事件块。
  buffer += decoder.decode();
  dispatchFinalEvent(buffer, onEvent);
}

/**
 * 记录浏览器底层读取到的流式字节片段。
 */
function logStreamReadChunk(readCount: number, byteLength: number, startTime: number): void {
  if (readCount !== 1 && readCount % 50 !== 0) {
    return;
  }

  // 只记录字节长度和耗时，避免日志泄露用户输入或模型正文。
  console.info('浏览器读取到 SSE 字节片段', {
    count: readCount,
    bytes: byteLength,
    elapsedMs: Math.round(performance.now() - startTime),
  });
}

/**
 * 派发缓冲区中的完整 SSE 事件。
 */
function dispatchBufferedEvents(buffer: string, onEvent: (event: StreamEvent) => void): string {
  const parts = buffer.split(/\r?\n\r?\n/);
  const remaining = parts.pop() ?? '';
  parts.forEach((part) => {
    const event = parseStreamEvent(part);
    if (event.data) {
      onEvent(event);
    }
  });
  return remaining;
}

/**
 * 解析单个 SSE 事件块。
 */
function parseStreamEvent(block: string): StreamEvent {
  let eventName = 'message';
  const dataLines: string[] = [];
  block.split(/\r?\n/).forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length).trim();
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).replace(/^ /, ''));
    }
  });
  return { event: eventName, data: dataLines.join('\n') };
}

/**
 * 派发流结束时剩余的最后事件。
 */
function dispatchFinalEvent(buffer: string, onEvent: (event: StreamEvent) => void): void {
  if (!buffer.trim()) {
    return;
  }
  const event = parseStreamEvent(buffer);
  if (event.data) {
    onEvent(event);
  }
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
