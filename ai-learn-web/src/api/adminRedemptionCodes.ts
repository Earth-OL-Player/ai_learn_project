import { del, get, getBlob, post, put } from './http';
import type { PageResponse } from '../types/page';

export type RedemptionCodeType =
  | 'PRO_MONTHLY'
  | 'SUPER_MONTHLY'
  | 'PRO_PERMANENT'
  | 'SUPER_PERMANENT'
  | 'PRO_PERMANENT_TO_SUPER';

export type RedemptionCodeStatus = 'UNUSED' | 'USED';

export interface AdminRedemptionCode {
  id: string;
  code: string;
  codeType: RedemptionCodeType;
  codeTypeText: string;
  status: RedemptionCodeStatus;
  statusText: string;
  usedByUserId: string;
  usedByUsername: string | null;
  usedAt: string | null;
  createdAt: string;
  editable: boolean;
  deletable: boolean;
}

export interface AdminRedemptionCodeQuery {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  codeType?: RedemptionCodeType | '';
  status?: RedemptionCodeStatus | '';
}

/**
 * 分页查询兑换码。
 */
export function fetchAdminRedemptionCodes(query: AdminRedemptionCodeQuery): Promise<PageResponse<AdminRedemptionCode>> {
  const params = buildQueryParams(query);
  return get<PageResponse<AdminRedemptionCode>>(`/admin/redemption-codes?${params.toString()}`);
}

/**
 * 批量生成兑换码。
 */
export function generateAdminRedemptionCodes(codeType: RedemptionCodeType, quantity: number): Promise<AdminRedemptionCode[]> {
  return post<AdminRedemptionCode[], { codeType: RedemptionCodeType; quantity: number }>('/admin/redemption-codes/generate', {
    codeType,
    quantity,
  });
}

/**
 * 更新未使用兑换码类型。
 */
export function updateAdminRedemptionCode(id: string, codeType: RedemptionCodeType): Promise<AdminRedemptionCode> {
  return put<AdminRedemptionCode, { codeType: RedemptionCodeType }>(`/admin/redemption-codes/${id}`, { codeType });
}

/**
 * 删除未使用兑换码。
 */
export function deleteAdminRedemptionCode(id: string): Promise<boolean> {
  return del<boolean>(`/admin/redemption-codes/${id}`);
}

/**
 * 导出兑换码。
 */
export function exportAdminRedemptionCodes(query: AdminRedemptionCodeQuery): Promise<Blob> {
  const params = buildQueryParams(query);
  return getBlob(`/admin/redemption-codes/export?${params.toString()}`);
}

/**
 * 构造查询参数。
 */
function buildQueryParams(query: AdminRedemptionCodeQuery): URLSearchParams {
  const params = new URLSearchParams();
  params.set('pageNo', String(query.pageNo));
  params.set('pageSize', String(query.pageSize));
  appendOptionalParam(params, 'keyword', query.keyword);
  appendOptionalParam(params, 'codeType', query.codeType);
  appendOptionalParam(params, 'status', query.status);
  return params;
}

/**
 * 追加可选参数。
 */
function appendOptionalParam(params: URLSearchParams, key: string, value?: string): void {
  if (value) {
    params.set(key, value);
  }
}
