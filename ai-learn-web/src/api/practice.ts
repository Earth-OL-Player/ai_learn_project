import { get, post } from './http';
import type { PageResponse } from '../types/page';
import type { AnswerRecordItem } from '../types/answer-record';
import type { GrowthInfo } from '../types/growth';
import type { PracticeQuestion, PracticeStartPayload, PracticeSubmitPayload, PracticeSubmitResult } from '../types/practice';

/**
 * 开始一次刷题。
 */
export function startPractice(payload: PracticeStartPayload): Promise<PracticeQuestion> {
  return post<PracticeQuestion, PracticeStartPayload>('/agent/practice/start', payload);
}

/**
 * 提交答案并获取评分。
 */
export function submitPractice(payload: PracticeSubmitPayload): Promise<PracticeSubmitResult> {
  return post<PracticeSubmitResult, PracticeSubmitPayload>('/agent/practice/submit', payload);
}

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
