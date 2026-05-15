import type { GrowthInfo } from './growth';

export interface PracticeStartPayload {
  difficulty?: string;
  questionType?: string;
  knowledgePointIds?: string[];
  sourceScope?: string;
}

export interface PracticeQuestion {
  sessionId: string;
  questionId: string;
  title: string;
  content: string;
  questionType: string;
  questionTypeText: string;
  difficulty: string;
  difficultyText: string;
  knowledgePoints: string[];
  sourceType: string;
  recommendReason: string;
}

export interface PracticeSubmitPayload {
  sessionId: string;
  questionId: string;
  userAnswer: string;
  durationSeconds: number;
}

export interface PracticeSubmitResult {
  answerRecordId: string;
  score: number;
  isCorrect: boolean;
  hitPoints: string[];
  missingPoints: string[];
  problems: string[];
  referenceAnswer: string;
  improvementAdvice: string;
  reviewKnowledgePoints: string[];
  gradingSource: string;
  growth: GrowthInfo;
}
