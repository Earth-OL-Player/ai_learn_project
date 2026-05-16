import { get } from './http';
import type { PageResponse } from '../types/page';
import type { AnswerRecordItem } from '../types/answer-record';
import type { GrowthInfo } from '../types/growth';

/**
 * 查询我的答题记录。
 */
export function fetchMyAnswerRecords(pageNo: number, pageSize: number): Promise<PageResponse<AnswerRecordItem>> {
  return get<PageResponse<AnswerRecordItem>>(`/answer-records/me?pageNo=${pageNo}&pageSize=${pageSize}`);
}

/**
 * 查询我的成长信息。
 */
export function fetchMyGrowth(): Promise<GrowthInfo> {
  return get<GrowthInfo>('/growth/me');
}
