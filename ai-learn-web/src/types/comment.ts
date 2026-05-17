import type { AuthorSummary } from './suggestion';

export interface CommentItem {
  id: string;
  content: string;
  parentId: string | null;
  likeCount: number;
  liked: boolean;
  replyCount: number;
  author: AuthorSummary;
  children: CommentItem[];
  createdAt: string | null;
}

export interface CreateCommentPayload {
  content: string;
  parentId?: string | null;
}
