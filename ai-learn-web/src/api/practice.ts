import { get, post, postStream, type StreamEvent } from './http';
import type { BadgeInfo, GrowthInfo } from '../types/growth';

export interface PracticeQuestion {
  code: string;
  question: string;
  questionType: string;
  importanceScore: number;
  occurrenceCount: number;
  answeredCount: number;
  bestScore: number;
}

export interface PracticeState {
  phase: PracticePhase;
  phaseText: string;
  currentQuestion: PracticeQuestion | null;
  lastScore: number | null;
  questionTypes: string[];
  growth: GrowthInfo;
}

export type PracticePhase = 'QUESTIONING' | 'ANSWERING' | 'DISCUSSING';
export type PracticeAction = 'QUESTION' | 'GRADING' | 'DISCUSSION' | 'TIP';

export interface PracticeGrading {
  score: number;
  hitPoints: string[];
  missingPoints: string[];
  problems: string[];
  referenceAnswer: string;
  improvementAdvice: string;
  reviewKnowledgePoints: string[];
  earnedExperience: number;
  previousBestScore: number;
  previousLastScore: number | null;
  experienceDetail: string;
  totalExperience: number;
  newBadges: BadgeInfo[];
  fallbackUsed: boolean;
}

export interface PracticeMessageResult {
  action: PracticeAction;
  phase: PracticePhase;
  message: string;
  question: PracticeQuestion | null;
  grading: PracticeGrading | null;
  growth: GrowthInfo;
}

export interface PracticeActionPayload {
  questionTypes: string[];
}

export interface PracticeMessagePayload extends PracticeActionPayload {
  content: string;
}

export interface PracticeMessageStreamHandlers {
  onMessageChunk: (chunk: string) => void;
  onResult: (result: PracticeMessageResult) => void;
}

/**
 * 查询题目分类。
 */
export function fetchPracticeCategories(): Promise<string[]> {
  return get<string[]>('/practice/categories');
}

/**
 * 查询当前刷题状态。
 */
export function fetchPracticeState(): Promise<PracticeState> {
  return get<PracticeState>('/practice/state');
}

/**
 * 抽取下一题。
 */
export function fetchNextPracticeQuestion(payload: PracticeActionPayload): Promise<PracticeMessageResult> {
  return post<PracticeMessageResult, PracticeActionPayload>('/practice/next-question', payload);
}

/**
 * 重新回答当前题。
 */
export function retryPracticeQuestion(): Promise<PracticeMessageResult> {
  return post<PracticeMessageResult>('/practice/retry');
}

/**
 * 发送刷题聊天消息并接收流式回复。
 */
export function sendPracticeMessageStream(
  payload: PracticeMessagePayload,
  handlers: PracticeMessageStreamHandlers,
): Promise<void> {
  return postStream<PracticeMessagePayload>('/practice/messages/stream', payload, (event: StreamEvent) => {
    if (event.event === 'message') {
      handlers.onMessageChunk(event.data);
      return;
    }
    if (event.event === 'result') {
      handlers.onResult(JSON.parse(event.data) as PracticeMessageResult);
      return;
    }
    if (event.event === 'error') {
      throw new Error(event.data || '发送失败');
    }
  });
}

/**
 * 查询我的成长信息。
 */
export function fetchMyGrowth(): Promise<GrowthInfo> {
  return get<GrowthInfo>('/growth/me');
}
