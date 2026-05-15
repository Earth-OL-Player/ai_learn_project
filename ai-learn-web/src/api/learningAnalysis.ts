import { get } from './http';
import type { LearningAnalysis } from '../types/learning-analysis';

/**
 * 查询我的学习分析。
 */
export function fetchMyLearningAnalysis(): Promise<LearningAnalysis> {
  return get<LearningAnalysis>('/learning-analysis/me');
}
