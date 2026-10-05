import { get, post, postStream, type StreamEvent } from './http';
import type { AssistantActionRequest, AssistantMessage, AssistantPublishResult, AssistantSnapshot, AssistantSubmitContext } from '../types/assistant';

/** 请求超时只中断等待，不重发；提交可能已成功，必须查询持久化结果。 */
async function assistantRequest<T>(task: (signal: AbortSignal) => Promise<T>, parent?: AbortSignal, timeout = 15000): Promise<T> {
  const controller = new AbortController();
  let timedOut = false;
  const cancel = () => controller.abort();
  parent?.addEventListener('abort', cancel, { once: true });
  if (parent?.aborted) controller.abort();
  const timer = window.setTimeout(() => { timedOut = true; controller.abort(); }, timeout);
  try { return await task(controller.signal); }
  catch (cause) { if (timedOut && !parent?.aborted) throw new Error('助手连接超时，请查看任务状态后继续，避免重复发布'); throw cause; }
  finally { clearTimeout(timer); parent?.removeEventListener('abort', cancel); }
}

const assistantPost = <T>(path: string, body?: unknown) => assistantRequest(signal => post<T>(path, body, signal));
export const createAssistantSession = () => assistantPost<{ id: string }>('/assistant/sessions');
export const fetchAssistantSession = (id: string) => assistantRequest(signal => get<{ id: string; messages: AssistantMessage[]; runs: AssistantSnapshot[] }>(`/assistant/sessions/${id}`, signal));
export const fetchAssistantRun = (id: string) => assistantRequest(signal => get<AssistantSnapshot>(`/assistant/runs/${id}`, signal));
export const assistantControl = (id: string, action: 'pause' | 'cancel', body: unknown) => assistantPost<AssistantSnapshot>(`/assistant/runs/${id}/${action}`, body);
export const acknowledgeAssistantAction = (id: string, body: AssistantActionRequest) => assistantPost<AssistantSnapshot>(`/assistant/runs/${id}/client-results`, body);
export const confirmAssistantOperation = (id: string, body: unknown) => assistantPost<AssistantSnapshot>(`/assistant/runs/${id}/confirm`, body);
export const updateAssistantDraft = (id: string, body: unknown) => assistantPost<AssistantSnapshot>(`/assistant/runs/${id}/draft`, body);

export function submitAssistantForm(context: AssistantSubmitContext): Promise<AssistantPublishResult> {
  return assistantPost(`/assistant/runs/${context.runId}/operations/${context.request.operationId}/submit`, context.request);
}

export function streamAssistant(path: string, body: unknown, receive: (event: StreamEvent) => void, signal: AbortSignal): Promise<void> {
  return assistantRequest(activeSignal => postStream(`/assistant/${path}`, body, receive, activeSignal), signal, 180000);
}
