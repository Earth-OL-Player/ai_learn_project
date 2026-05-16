import { del, get, getBlob, post, postForm, put } from './http';
import type { PageResponse } from '../types/page';
import type { ImportSystemQuestionsResult, SystemQuestionItem, SystemQuestionPayload, SystemQuestionQuery } from '../types/system-question';

/**
 * 分页查询系统题库。
 */
export function fetchSystemQuestions(query: SystemQuestionQuery): Promise<PageResponse<SystemQuestionItem>> {
  const params = new URLSearchParams();
  params.set('pageNo', String(query.pageNo));
  params.set('pageSize', String(query.pageSize));
  appendOptionalParam(params, 'keyword', query.keyword);
  appendOptionalParam(params, 'questionType', query.questionType);
  return get<PageResponse<SystemQuestionItem>>(`/admin/system-questions?${params.toString()}`);
}

/**
 * 查询系统题目分类。
 */
export function fetchSystemQuestionTypes(): Promise<string[]> {
  return get<string[]>('/admin/system-questions/types');
}

/**
 * 新增系统题目。
 */
export function createSystemQuestion(payload: SystemQuestionPayload): Promise<SystemQuestionItem> {
  return post<SystemQuestionItem, SystemQuestionPayload>('/admin/system-questions', payload);
}

/**
 * 更新系统题目。
 */
export function updateSystemQuestion(id: string, payload: SystemQuestionPayload): Promise<SystemQuestionItem> {
  return put<SystemQuestionItem, SystemQuestionPayload>(`/admin/system-questions/${id}`, payload);
}

/**
 * 删除系统题目。
 */
export function deleteSystemQuestion(id: string): Promise<boolean> {
  return del<boolean>(`/admin/system-questions/${id}`);
}

/**
 * 下载系统题库 CSV 模板。
 */
export function downloadSystemQuestionTemplate(): Promise<Blob> {
  return getBlob('/admin/system-questions/template');
}

/**
 * 导入系统题库 CSV。
 */
export function importSystemQuestions(file: File): Promise<ImportSystemQuestionsResult> {
  const formData = new FormData();
  formData.append('file', file);
  return postForm<ImportSystemQuestionsResult>('/admin/system-questions/import', formData);
}

/**
 * 追加可选查询参数。
 */
function appendOptionalParam(params: URLSearchParams, key: string, value?: string): void {
  if (value) {
    params.set(key, value);
  }
}
