export interface AnswerRecordItem {
  id: string;
  questionId: string;
  questionTitle: string;
  questionType: string;
  questionTypeText: string;
  difficulty: string;
  difficultyText: string;
  score: number;
  isCorrect: boolean;
  improvementAdvice: string;
  durationSeconds: number | null;
  firstAttempt: boolean;
  createdAt: string;
}
