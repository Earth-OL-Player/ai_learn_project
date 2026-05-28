import { get, post } from './http';
import { buildQueryPath } from './queryParams';
import type { PageResponse } from '../types/page';
import type { CreateSuggestionPayload, SuggestionItem } from '../types/suggestion';

/**
 * 分页查询建议列表。
 */
export function fetchSuggestions(pageNo: number, pageSize: number, sort: string): Promise<PageResponse<SuggestionItem>> {
  return get<PageResponse<SuggestionItem>>(buildQueryPath('/suggestions', { pageNo, pageSize, sort }));
}

/**
 * 提交当前登录用户的建议。
 */
export function createSuggestion(payload: CreateSuggestionPayload): Promise<SuggestionItem> {
  return post<SuggestionItem, CreateSuggestionPayload>('/suggestions', payload);
}

/**
 * 点赞或取消点赞建议。
 */
export function toggleSuggestionLike(suggestionId: string): Promise<SuggestionItem> {
  return post<SuggestionItem>(`/suggestions/${suggestionId}/like`);
}
