import type { PracticeGrading, PracticePhase, PracticeQuestion } from '../../api/practice';

export interface ChatMessage {
  id: number;
  role: 'assistant' | 'user';
  text: string;
  question?: PracticeQuestion | null;
  grading?: PracticeGrading | null;
  streaming?: boolean;
  loadingText?: string;
}

export interface PracticeChatSnapshot {
  phase: PracticePhase;
  questionCode: string;
  messages: ChatMessage[];
  scrollTop?: number;
  pinnedToBottom?: boolean;
  updatedAt: number;
}
