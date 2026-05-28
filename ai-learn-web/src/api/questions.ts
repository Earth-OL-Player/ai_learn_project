import { get, getPublic } from './http';
import { buildQueryPath } from './queryParams';
import type { PageResponse } from '../types/page';
import type { QuestionDetail, QuestionListItem, QuestionQuery } from '../types/question';

/**
 * 分页查询题目列表。
 */
export function fetchQuestions(query: QuestionQuery): Promise<PageResponse<QuestionListItem>> {
  return get<PageResponse<QuestionListItem>>(buildQueryPath('/questions', {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    keyword: query.keyword,
    questionType: query.questionType,
  }));
}

/**
 * 查询题目详情。
 */
export function fetchQuestionDetail(id: string): Promise<QuestionDetail> {
  return get<QuestionDetail>(`/questions/${id}`);
}

/**
 * 查询热门面试题阅读文档。
 */
export function fetchInterviewQuestionDocument(questionType?: string): Promise<QuestionDetail[]> {
  // 热门面试题对游客开放阅读，公开接口不携带过期 token，避免游客场景被误拦截。
  return getPublic<QuestionDetail[]>(buildQueryPath('/public/questions/interview-document', { questionType }));
}

/**
 * 查询热门面试题公开分类列表。
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
