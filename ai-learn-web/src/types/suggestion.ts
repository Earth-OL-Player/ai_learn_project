export interface AuthorSummary {
  id: string;
  username: string;
  nickname: string | null;
  avatar: string | null;
  level: string;
  levelValue: number;
  rank: string;
}

export interface SuggestionItem {
  id: string;
  content: string;
  type: string;
  typeText: string;
  likeCount: number;
  liked: boolean;
  author: AuthorSummary;
  createdAt: string | null;
}

export interface CreateSuggestionPayload {
  type: string;
  content: string;
}
