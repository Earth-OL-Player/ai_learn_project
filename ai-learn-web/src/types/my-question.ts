export interface MyQuestionItem {
  id: string;
  title: string;
  content: string;
  questionType: string;
  questionTypeText: string;
  difficulty: string;
  difficultyText: string;
  tags: string[];
  knowledgePoints: string[];
  standardAnswer: string;
  analysis: string | null;
  createdAt: string;
}

export interface MyQuestionQuery {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  difficulty?: string;
  questionType?: string;
}

export interface MyQuestionPayload {
  title: string;
  content: string;
  questionType: string;
  difficulty: string;
  tags: string[];
  knowledgePoints: string[];
  standardAnswer: string;
  analysis?: string;
}

export interface ImportMyQuestionsPayload {
  mode: 'APPEND' | 'REPLACE';
  questions: MyQuestionPayload[];
}

export interface ImportMyQuestionsResult {
  importedCount: number;
  mode: string;
}
