import { get } from './http';
import type { LearningRoadmap } from '../types/learning';

/**
 * 查询 AI 应用开发学习路线。
 */
export function getLearningRoadmap(): Promise<LearningRoadmap> {
  return get<LearningRoadmap>('/learning/roadmap');
}
