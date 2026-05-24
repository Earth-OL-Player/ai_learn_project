import { get, post } from './http';

export type ModelLevel = 'BASIC' | 'PRO' | 'SUPER';

export interface ModelEntitlementStatus {
  level: ModelLevel;
  levelText: string;
  modelName: string;
  remainingDays: number;
  remainingDaysText: string;
  permanent: boolean;
  authorizationVisible: boolean;
  authorizationButtonText: string;
  authorizationUrl: string;
  authorizationConfigured: boolean;
  frozenTip: string;
  frozenProRemainingDays: number;
}

export interface RedeemModelCodeResponse {
  message: string;
  entitlement: ModelEntitlementStatus;
}

/**
 * 查询当前模型权益。
 */
export function fetchModelEntitlementStatus(): Promise<ModelEntitlementStatus> {
  return get<ModelEntitlementStatus>('/model-entitlements/status');
}

/**
 * 兑换模型权益兑换码。
 */
export function redeemModelCode(code: string): Promise<RedeemModelCodeResponse> {
  return post<RedeemModelCodeResponse, { code: string }>('/model-entitlements/redeem', { code });
}
