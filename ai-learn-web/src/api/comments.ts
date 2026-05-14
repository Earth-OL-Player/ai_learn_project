import { get, post } from './http';
import type { CommentItem, CreateCommentPayload } from '../types/comment';
import type { PageResponse } from '../types/page';

/**
 * 分页查询评论列表。
 */
export function fetchComments(pageNo: number, pageSize: number): Promise<PageResponse<CommentItem>> {
  return get<PageResponse<CommentItem>>(`/comments?pageNo=${pageNo}&pageSize=${pageSize}`);
}

/**
 * 发表当前登录用户的评论。
 */
export function createComment(payload: CreateCommentPayload): Promise<CommentItem> {
  return post<CommentItem, CreateCommentPayload>('/comments', payload);
}
