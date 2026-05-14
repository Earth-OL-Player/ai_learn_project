export interface KnowledgePointItem {
  id: string;
  name: string;
  description: string | null;
}

export interface QuestionListItem {
  id: string;
  title: string;
  questionType: string;
  questionTypeText: string;
  difficulty: string;
  difficultyText: string;
  tags: string[];
  knowledgePoints: string[];
  createdAt: string;
}

export interface QuestionDetail extends QuestionListItem {
  content: string;
  standardAnswer: string;
  analysis: string | null;
}

export interface QuestionQuery {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  difficulty?: string;
  questionType?: string;
  knowledgePointId?: string;
}
