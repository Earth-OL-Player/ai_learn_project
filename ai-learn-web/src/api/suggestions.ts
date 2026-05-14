import { get, post } from './http';
import type { PageResponse } from '../types/page';
import type { CreateSuggestionPayload, SuggestionItem } from '../types/suggestion';

/**
 * 分页查询建议列表。
 */
export function fetchSuggestions(pageNo: number, pageSize: number): Promise<PageResponse<SuggestionItem>> {
  return get<PageResponse<SuggestionItem>>(`/suggestions?pageNo=${pageNo}&pageSize=${pageSize}`);
}

/**
 * 提交当前登录用户的建议。
 */
export function createSuggestion(payload: CreateSuggestionPayload): Promise<SuggestionItem> {
  return post<SuggestionItem, CreateSuggestionPayload>('/suggestions', payload);
}
