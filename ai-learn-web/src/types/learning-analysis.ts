export interface LearningAnalysis {
  weakPoints: WeakPoint[];
}

export interface WeakPoint {
  knowledgePointId: string;
  knowledgePointName: string;
  answeredCount: number;
  averageScore: number;
  lowScoreCount: number;
  recommendedQuestionId: string | null;
  recommendedQuestionTitle: string | null;
  advice: string;
}
