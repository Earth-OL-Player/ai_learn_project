import { del, get, getBlob, post, put } from './http';
import { buildQueryPath, type QueryParams } from './queryParams';
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
  return get<PageResponse<AdminRedemptionCode>>(buildQueryPath('/admin/redemption-codes', buildRedemptionCodeQuery(query)));
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
  return getBlob(buildQueryPath('/admin/redemption-codes/export', buildRedemptionCodeQuery(query)));
}

/**
 * 构造兑换码查询参数。
 */
function buildRedemptionCodeQuery(query: AdminRedemptionCodeQuery): QueryParams {
  return {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    keyword: query.keyword,
    codeType: query.codeType,
    status: query.status,
  };
}
