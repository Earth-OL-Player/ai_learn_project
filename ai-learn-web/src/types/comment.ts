import type { AuthorSummary } from './suggestion';

export interface CommentItem {
  id: string;
  content: string;
  parentId: string | null;
  likeCount: number;
  author: AuthorSummary;
  createdAt: string | null;
}

export interface CreateCommentPayload {
  content: string;
  parentId?: string | null;
}
