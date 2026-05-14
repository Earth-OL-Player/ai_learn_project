import { get } from './http';
import type { PageResponse } from '../types/page';
import type { KnowledgePointItem, QuestionDetail, QuestionListItem, QuestionQuery } from '../types/question';

/**
 * 分页查询题目列表。
 */
export function fetchQuestions(query: QuestionQuery): Promise<PageResponse<QuestionListItem>> {
  const params = new URLSearchParams();
  params.set('pageNo', String(query.pageNo));
  params.set('pageSize', String(query.pageSize));
  appendOptionalParam(params, 'keyword', query.keyword);
  appendOptionalParam(params, 'difficulty', query.difficulty);
  appendOptionalParam(params, 'questionType', query.questionType);
  appendOptionalParam(params, 'knowledgePointId', query.knowledgePointId);
  return get<PageResponse<QuestionListItem>>(`/questions?${params.toString()}`);
}

/**
 * 查询题目详情。
 */
export function fetchQuestionDetail(id: string): Promise<QuestionDetail> {
  return get<QuestionDetail>(`/questions/${id}`);
}

/**
 * 查询知识点列表。
 */
export function fetchKnowledgePoints(): Promise<KnowledgePointItem[]> {
  return get<KnowledgePointItem[]>('/knowledge-points');
}

/**
 * 追加可选查询参数。
 */
function appendOptionalParam(params: URLSearchParams, key: string, value?: string): void {
  if (value) {
    params.set(key, value);
  }
}
