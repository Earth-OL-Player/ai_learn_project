import { del, get, post } from './http';
import type { PageResponse } from '../types/page';
import type { ImportMyQuestionsPayload, ImportMyQuestionsResult, MyQuestionItem, MyQuestionPayload, MyQuestionQuery } from '../types/my-question';

/**
 * 分页查询我的题库。
 */
export function fetchMyQuestions(query: MyQuestionQuery): Promise<PageResponse<MyQuestionItem>> {
  const params = new URLSearchParams();
  params.set('pageNo', String(query.pageNo));
  params.set('pageSize', String(query.pageSize));
  appendOptionalParam(params, 'keyword', query.keyword);
  appendOptionalParam(params, 'difficulty', query.difficulty);
  appendOptionalParam(params, 'questionType', query.questionType);
  return get<PageResponse<MyQuestionItem>>(`/my-questions?${params.toString()}`);
}

/**
 * 新增个人题目。
 */
export function createMyQuestion(payload: MyQuestionPayload): Promise<MyQuestionItem> {
  return post<MyQuestionItem, MyQuestionPayload>('/my-questions', payload);
}

/**
 * 批量导入个人题库。
 */
export function importMyQuestions(payload: ImportMyQuestionsPayload): Promise<ImportMyQuestionsResult> {
  return post<ImportMyQuestionsResult, ImportMyQuestionsPayload>('/my-questions/import', payload);
}

/**
 * 删除个人题目。
 */
export function deleteMyQuestion(id: string): Promise<boolean> {
  return del<boolean>(`/my-questions/${id}`);
}

/**
 * 追加可选查询参数。
 */
function appendOptionalParam(params: URLSearchParams, key: string, value?: string): void {
  if (value) {
    params.set(key, value);
  }
}
