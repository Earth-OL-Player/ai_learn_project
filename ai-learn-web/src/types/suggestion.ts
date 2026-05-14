export interface AuthorSummary {
  id: string;
  username: string;
  nickname: string | null;
  avatar: string | null;
}

export interface SuggestionItem {
  id: string;
  title: string;
  content: string;
  type: string;
  typeText: string;
  status: string;
  statusText: string;
  author: AuthorSummary;
  createdAt: string | null;
}

export interface CreateSuggestionPayload {
  title: string;
  type: string;
  content: string;
}
