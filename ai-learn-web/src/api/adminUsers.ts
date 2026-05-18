import { del, get, post, put } from './http';
import type { PageResponse } from '../types/page';
import type { AdminUserItem, AdminUserPayload, AdminUserQuery, UserLimitInfo } from '../types/admin-user';

/**
 * 分页查询用户。
 */
export function fetchAdminUsers(query: AdminUserQuery): Promise<PageResponse<AdminUserItem>> {
  const params = new URLSearchParams();
  params.set('pageNo', String(query.pageNo));
  params.set('pageSize', String(query.pageSize));
  appendOptionalParam(params, 'keyword', query.keyword);
  return get<PageResponse<AdminUserItem>>(`/admin/users?${params.toString()}`);
}

/**
 * 查询系统用户数量限制。
 */
export function fetchUserLimit(): Promise<UserLimitInfo> {
  return get<UserLimitInfo>('/admin/users/limit');
}

/**
 * 更新系统用户数量限制。
 */
export function updateUserLimit(maxUsers: number): Promise<UserLimitInfo> {
  return put<UserLimitInfo, { maxUsers: number }>('/admin/users/limit', { maxUsers });
}

/**
 * 新增用户。
 */
export function createAdminUser(payload: AdminUserPayload): Promise<AdminUserItem> {
  return post<AdminUserItem, AdminUserPayload>('/admin/users', payload);
}

/**
 * 更新用户。
 */
export function updateAdminUser(id: string, payload: AdminUserPayload): Promise<AdminUserItem> {
  return put<AdminUserItem, AdminUserPayload>(`/admin/users/${id}`, payload);
}

/**
 * 删除用户。
 */
export function deleteAdminUser(id: string): Promise<boolean> {
  return del<boolean>(`/admin/users/${id}`);
}

/**
 * 追加可选查询参数。
 */
function appendOptionalParam(params: URLSearchParams, key: string, value?: string): void {
  if (value) {
    params.set(key, value);
  }
}
