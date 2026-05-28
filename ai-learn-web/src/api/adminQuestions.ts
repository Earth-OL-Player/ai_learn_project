import { del, get, getBlob, post, postForm, put } from './http';
import { buildQueryPath } from './queryParams';
import type { PageResponse } from '../types/page';
import type {
  ImportSystemQuestionsPrecheckResult,
  ImportSystemQuestionsResult,
  SystemQuestionItem,
  SystemQuestionPayload,
  SystemQuestionQuery,
} from '../types/system-question';

/**
 * 分页查询系统题库。
 */
export function fetchSystemQuestions(query: SystemQuestionQuery): Promise<PageResponse<SystemQuestionItem>> {
  return get<PageResponse<SystemQuestionItem>>(buildQueryPath('/admin/system-questions', {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    keyword: query.keyword,
    questionType: query.questionType,
  }));
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
 * 一键清空当前系统题库。
 */
export function clearSystemQuestions(): Promise<boolean> {
  return del<boolean>('/admin/system-questions/clear');
}

/**
 * 下载系统题库 CSV 模板。
 */
export function downloadSystemQuestionTemplate(): Promise<Blob> {
  return getBlob('/admin/system-questions/template');
}

/**
 * 预检系统题库 CSV。
 */
export function precheckImportSystemQuestions(file: File): Promise<ImportSystemQuestionsPrecheckResult> {
  return postForm<ImportSystemQuestionsPrecheckResult>('/admin/system-questions/import/precheck', buildCsvImportForm(file));
}

/**
 * 导入系统题库 CSV。
 */
export function importSystemQuestions(file: File): Promise<ImportSystemQuestionsResult> {
  return postForm<ImportSystemQuestionsResult>('/admin/system-questions/import', buildCsvImportForm(file));
}

/**
 * 构造系统题库 CSV 上传表单。
 */
function buildCsvImportForm(file: File): FormData {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
}
