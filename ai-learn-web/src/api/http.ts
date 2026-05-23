import { API_SUCCESS_CODE, DEFAULT_API_BASE_URL, REFRESH_TOKEN_HEADER, TRACE_ID_HEADER } from '../constants/api';
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

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly traceId?: string;

  /**
   * 创建接口错误对象。
   */
  constructor(message: string, status: number, code: string, traceId?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.traceId = traceId;
  }
}

const AUTH_UNAUTHORIZED_CODE = 'AUTH_UNAUTHORIZED';
const HTTP_ERROR_CODE_PREFIX = 'HTTP_';

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
 * 生成浏览器侧请求 traceId。
 */
function generateTraceId(): string {
  const cryptoApi = globalThis.crypto;
  if (cryptoApi?.randomUUID) {
    return cryptoApi.randomUUID().replace(/-/g, '');
  }

  // 兼容少数不支持 randomUUID 的浏览器环境。
  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}`.slice(0, 32);
}

/**
 * 构造请求头，自动追加登录令牌。
 */
function buildHeaders(extraHeaders?: HeadersInit, includeAuthToken = true, traceId = generateTraceId()): HeadersInit {
  const headers = new Headers(extraHeaders);
  const token = getStoredAccessToken();

  // 登录后统一通过 Bearer token 访问受保护接口。
  headers.set('Accept', headers.get('Accept') || 'application/json');
  headers.set(TRACE_ID_HEADER, headers.get(TRACE_ID_HEADER) || traceId);
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
 * 解析后端错误响应体。
 */
async function parseErrorBody(response: Response): Promise<ApiResponse<unknown> | undefined> {
  try {
    return (await response.json()) as ApiResponse<unknown>;
  } catch {
    return undefined;
  }
}

/**
 * 根据 HTTP 状态生成兜底错误消息。
 */
function resolveStatusMessage(status: number): string {
  if (status === 400) {
    return '请求参数不正确，请检查后重试';
  }
  if (status === 401) {
    return '登录状态已失效，请重新登录';
  }
  if (status === 403) {
    return '当前账号暂无权限执行该操作';
  }
  if (status === 404) {
    return '请求的资源不存在';
  }
  if (status === 409) {
    return '数据已存在或状态冲突，请刷新后重试';
  }
  return '服务暂时不可用，请稍后重试';
}

/**
 * 判断是否为认证失效错误。
 */
function isUnauthorizedError(status: number, code: string): boolean {
  return status === 401 || code === AUTH_UNAUTHORIZED_CODE;
}

/**
 * 认证失效时同步清理前端登录态。
 */
function clearAuthWhenUnauthorized(status: number, code: string): void {
  if (isUnauthorizedError(status, code)) {
    clearStoredAccessToken();
  }
}

/**
 * 抛出携带 HTTP 状态和业务码的接口错误。
 */
async function throwApiError(response: Response): Promise<never> {
  const result = await parseErrorBody(response);
  const code = result?.code || `${HTTP_ERROR_CODE_PREFIX}${response.status}`;
  const message = result?.message || resolveStatusMessage(response.status);

  // 新后端使用 HTTP 状态识别错误，旧响应仍通过业务码兼容登录态清理。
  clearAuthWhenUnauthorized(response.status, code);
  throw new ApiError(message, response.status, code, result?.traceId);
}

/**
 * 解析后端统一响应。
 */
async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    await throwApiError(response);
  }

  saveRefreshedToken(response);

  const result = (await response.json()) as ApiResponse<T>;
  if (result.code !== API_SUCCESS_CODE) {
    clearAuthWhenUnauthorized(response.status, result.code);
    throw new ApiError(result.message || '请求处理失败', response.status, result.code, result.traceId);
  }
  return result.data;
}

/**
 * 发起 HTTP 请求并处理统一响应。
 */
async function request<T>(path: string, init: RequestInit, includeAuthToken = true): Promise<T> {
  const traceId = generateTraceId();
  const startTime = performance.now();
  try {
    const response = await fetch(buildRequestUrl(path), {
      ...init,
      headers: buildHeaders(init.headers, includeAuthToken, traceId),
    });
    logBrowserRequest(path, traceId, response.status, startTime);
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
  const traceId = generateTraceId();
  const startTime = performance.now();
  const response = await fetch(buildRequestUrl(path), {
    method: 'GET',
    headers: buildHeaders({ Accept: 'text/csv' }, true, traceId),
  });
  logBrowserRequest(path, traceId, response.status, startTime);
  if (!response.ok) {
    await throwApiError(response);
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
  const traceId = generateTraceId();
  const startTime = performance.now();
  const response = await fetch(buildRequestUrl(path), {
    method: 'POST',
    headers: buildHeaders({
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
    }, true, traceId),
    body: JSON.stringify(body),
  });
  logBrowserRequest(path, traceId, response.status, startTime);
  const responseBody = response.body;

  // 流式接口仍复用后端自动续期 token。
  if (!response.ok) {
    await throwApiError(response);
  }
  if (!responseBody) {
    throw new ApiError(
      '服务暂时不可用，请稍后重试',
      response.status,
      `${HTTP_ERROR_CODE_PREFIX}${response.status}`,
    );
  }
  saveRefreshedToken(response);
  await readEventStream(responseBody, onEvent, traceId);
}

/**
 * 读取 SSE 响应流。
 */
async function readEventStream(
  stream: ReadableStream<Uint8Array>,
  onEvent: (event: StreamEvent) => void,
  traceId: string,
): Promise<void> {
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
    logStreamReadChunk(readCount, value.byteLength, startTime, traceId);
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
function logStreamReadChunk(readCount: number, byteLength: number, startTime: number, traceId: string): void {
  if (readCount !== 1 && readCount % 50 !== 0) {
    return;
  }

  // 只记录字节长度和耗时，避免日志泄露用户输入或模型正文。
  console.info('浏览器读取到 SSE 字节片段', {
    traceId,
    count: readCount,
    bytes: byteLength,
    elapsedMs: Math.round(performance.now() - startTime),
  });
}

/**
 * 记录浏览器到 Java 后端的请求观测信息。
 */
function logBrowserRequest(path: string, traceId: string, status: number, startTime: number): void {
  console.info('浏览器 API 请求完成', {
    traceId,
    path,
    status,
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
