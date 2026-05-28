import { get, post } from './http';
import { buildQueryPath } from './queryParams';
import type { CommentItem, CreateCommentPayload } from '../types/comment';
import type { PageResponse } from '../types/page';

/**
 * 分页查询评论列表。
 */
export function fetchComments(pageNo: number, pageSize: number, sort: string): Promise<PageResponse<CommentItem>> {
  return get<PageResponse<CommentItem>>(buildQueryPath('/comments', { pageNo, pageSize, sort }));
}

/**
 * 发表当前登录用户的评论。
 */
export function createComment(payload: CreateCommentPayload): Promise<CommentItem> {
  return post<CommentItem, CreateCommentPayload>('/comments', payload);
}

/**
 * 点赞或取消点赞评论。
 */
export function toggleCommentLike(commentId: string): Promise<CommentItem> {
  return post<CommentItem>(`/comments/${commentId}/like`);
}
