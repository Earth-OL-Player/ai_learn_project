export interface SystemQuestionItem {
  id: string;
  code: string;
  question: string;
  questionType: string;
  standardAnswer: string;
  importanceScore: number;
  /** 真实面试出现次数。 */
  occurrenceCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface SystemQuestionQuery {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  questionType?: string;
}

export interface SystemQuestionPayload {
  code?: string;
  question: string;
  questionType: string;
  standardAnswer: string;
  importanceScore: number;
  /** 真实面试出现次数。 */
  occurrenceCount: number;
}

export interface ImportSystemQuestionsResult {
  importedCount: number;
  createdCount: number;
  updatedCount: number;
}
