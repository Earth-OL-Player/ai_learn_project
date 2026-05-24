import { get, put } from './http';
import type { ModelLevel } from './modelEntitlements';

export interface AdminModelConfig {
  level: ModelLevel;
  levelText: string;
  modelName: string;
  baseUrl: string;
  apiKey: string;
  updatedAt: string | null;
}

export interface AdminModelConfigPayload {
  modelName: string;
  baseUrl: string;
  apiKey: string;
}

/**
 * 查询管理员模型配置。
 */
export function fetchAdminModelConfigs(): Promise<AdminModelConfig[]> {
  return get<AdminModelConfig[]>('/admin/model-configs');
}

/**
 * 保存指定等级模型配置。
 */
export function saveAdminModelConfig(level: ModelLevel, payload: AdminModelConfigPayload): Promise<AdminModelConfig> {
  return put<AdminModelConfig, AdminModelConfigPayload>(`/admin/model-configs/${level}`, payload);
}
