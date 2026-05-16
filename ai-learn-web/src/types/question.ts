export interface KnowledgePointItem {
  id: string;
  name: string;
  description: string | null;
}

export interface QuestionListItem {
  id: string;
  code: string;
  question: string;
  questionType: string;
  questionTypeText: string;
  knowledgePoints: string[];
  importanceScore: number;
  /** 真实面试出现次数。 */
  occurrenceCount: number;
  createdAt: string;
}

export interface QuestionDetail extends QuestionListItem {
  standardAnswer: string;
}

export interface QuestionQuery {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  questionType?: string;
  knowledgePointId?: string;
}
