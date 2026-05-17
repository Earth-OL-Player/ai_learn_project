import { get, getPublic } from './http';
import type { PageResponse } from '../types/page';
import type { QuestionDetail, QuestionListItem, QuestionQuery } from '../types/question';

/**
 * 分页查询题目列表。
 */
export function fetchQuestions(query: QuestionQuery): Promise<PageResponse<QuestionListItem>> {
  const params = new URLSearchParams();
  params.set('pageNo', String(query.pageNo));
  params.set('pageSize', String(query.pageSize));
  appendOptionalParam(params, 'keyword', query.keyword);
  appendOptionalParam(params, 'questionType', query.questionType);
  return get<PageResponse<QuestionListItem>>(`/questions?${params.toString()}`);
}

/**
 * 查询题目详情。
 */
export function fetchQuestionDetail(id: string): Promise<QuestionDetail> {
  return get<QuestionDetail>(`/questions/${id}`);
}

/**
 * 查询热门面经阅读文档。
 */
export function fetchInterviewQuestionDocument(questionType?: string): Promise<QuestionDetail[]> {
  const params = new URLSearchParams();
  appendOptionalParam(params, 'questionType', questionType);
  const queryString = params.toString();

  // 热门面经对游客开放阅读，公开接口不携带过期 token，避免游客场景被误拦截。
  return getPublic<QuestionDetail[]>(`/public/questions/interview-document${queryString ? `?${queryString}` : ''}`);
}

/**
 * 查询热门面经公开分类列表。
 */
export function fetchPublicQuestionTypes(): Promise<string[]> {
  return getPublic<string[]>('/public/questions/types');
}

/**
 * 查询题目分类列表。
 */
export function fetchQuestionTypes(): Promise<string[]> {
  return get<string[]>('/questions/types');
}

/**
 * 追加可选查询参数。
 */
function appendOptionalParam(params: URLSearchParams, key: string, value?: string): void {
  if (value) {
    params.set(key, value);
  }
}
