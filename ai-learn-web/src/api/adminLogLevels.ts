import { get, put } from './http';

export type AdminLogLevelValue = 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

export interface AdminLogLevelItem {
  service: string;
  serviceName: string;
  level: string;
  available: boolean;
  message: string;
  updatedAt: string;
}

export interface AdminLogLevelPayload {
  level: AdminLogLevelValue;
}

/**
 * 查询管理员日志级别列表。
 */
export function fetchAdminLogLevels(): Promise<AdminLogLevelItem[]> {
  return get<AdminLogLevelItem[]>('/admin/log-levels');
}

/**
 * 更新指定服务日志级别。
 */
export function updateAdminLogLevel(service: string, payload: AdminLogLevelPayload): Promise<AdminLogLevelItem> {
  return put<AdminLogLevelItem, AdminLogLevelPayload>(`/admin/log-levels/${service}`, payload);
}
