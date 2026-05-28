import { get, put } from './http';
import { buildQueryPath } from './queryParams';
import type { PageResponse } from '../types/page';

export type GenderCode = 'MALE' | 'FEMALE';

export interface CurrentUser {
  id: string;
  username: string;
  nickname: string | null;
  avatar: string | null;
  gender: GenderCode | null;
  motto: string | null;
  email: string | null;
  experience: number;
  level: string;
  levelName: string;
  rank: string;
  levelValue: number;
  currentLevelExperience: number;
  nextLevelExperience: number;
  experienceToNextLevel: number;
  levelProgressText: string;
  superAdmin: boolean;
  createdAt: string;
}

export interface UpdateProfilePayload {
  nickname: string;
  gender?: GenderCode | null;
  motto?: string | null;
}

export interface UserQuestionStatsQuery {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  questionType?: string;
}

export interface UserQuestionStatsItem {
  questionCode: string;
  question: string;
  questionType: string;
  answerCount: number;
  bestScore: number;
  lastScore: number;
  firstAnsweredAt: string | null;
  lastAnsweredAt: string | null;
}

export interface UserQuestionTypeStats {
  questionType: string;
  questionCount: number;
  answerCount: number;
  averageBestScore: number;
  averageLastScore: number;
  weakCount: number;
}

export interface UserQuestionStatsOverview {
  practicedQuestionCount: number;
  totalAnswerCount: number;
  averageBestScore: number;
  averageLastScore: number;
  weakQuestionCount: number;
  lastAnsweredAt: string | null;
  questionTypes: string[];
  typeStats: UserQuestionTypeStats[];
}

/**
 * 查询当前登录用户。
 */
export function getCurrentUser(): Promise<CurrentUser> {
  return get<CurrentUser>('/users/me');
}

/**
 * 更新当前登录用户资料。
 */
export function updateCurrentProfile(payload: UpdateProfilePayload): Promise<CurrentUser> {
  return put<CurrentUser, UpdateProfilePayload>('/users/me/profile', payload);
}

/**
 * 查询当前用户智能刷题记录列表。
 */
export function fetchCurrentUserQuestionStats(query: UserQuestionStatsQuery): Promise<PageResponse<UserQuestionStatsItem>> {
  return get<PageResponse<UserQuestionStatsItem>>(buildQueryPath('/users/me/question-stats', {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    keyword: query.keyword,
    questionType: query.questionType,
  }));
}

/**
 * 查询当前用户智能刷题记录概览。
 */
export function fetchCurrentUserQuestionStatsOverview(): Promise<UserQuestionStatsOverview> {
  return get<UserQuestionStatsOverview>('/users/me/question-stats/overview');
}
