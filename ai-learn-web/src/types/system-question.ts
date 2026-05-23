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
  skippedCount: number;
  conflictCount: number;
  errorCount: number;
  rows: ImportSystemQuestionPreviewRow[];
  issues: ImportSystemQuestionIssue[];
}

export interface ImportSystemQuestionsPrecheckResult {
  totalCount: number;
  importableCount: number;
  createdCount: number;
  updatedCount: number;
  conflictCount: number;
  errorCount: number;
  rows: ImportSystemQuestionPreviewRow[];
  issues: ImportSystemQuestionIssue[];
}

export interface ImportSystemQuestionPreviewRow {
  rowIndex: number;
  action: ImportSystemQuestionAction;
  actionText: string;
  importable: boolean;
  code: string;
  question: string;
  questionType: string;
  standardAnswer: string;
  importanceScore: number | null;
  occurrenceCount: number | null;
  diffs: ImportSystemQuestionDiff[];
  issues: ImportSystemQuestionIssue[];
}

export interface ImportSystemQuestionDiff {
  fieldName: string;
  fieldLabel: string;
  oldValue: string;
  newValue: string;
}

export interface ImportSystemQuestionIssue {
  rowIndex: number;
  fieldName: string;
  fieldLabel: string;
  message: string;
}

export type ImportSystemQuestionAction = 'CREATE' | 'UPDATE' | 'CONFLICT' | 'ERROR';
