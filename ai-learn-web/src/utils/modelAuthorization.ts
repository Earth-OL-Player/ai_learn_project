import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { fetchModelEntitlementStatus, type ModelEntitlementStatus } from '../api/modelEntitlements';

const WEBSITE_PROTOCOLS = ['http:', 'https:'];

/**
 * 打开模型授权入口。
 */
export async function openModelAuthorization(status?: ModelEntitlementStatus | null): Promise<void> {
  const currentStatus = status || await fetchModelEntitlementStatus();
  const authorizationUrl = normalizeAuthorizationUrl(currentStatus.authorizationUrl);
  if (!currentStatus.authorizationConfigured || !authorizationUrl) {
    ElMessage.warning('授权入口暂未开放');
    return;
  }

  // 使用新标签页打开授权入口，避免打断当前刷题或资料阅读状态。
  window.open(authorizationUrl, '_blank', 'noopener,noreferrer');
}

/**
 * 校验并规整后端配置的完整网站地址。
 */
function normalizeAuthorizationUrl(url: string): string {
  const safeUrl = url.trim();
  if (!safeUrl) {
    return '';
  }

  // 授权入口只接受完整 http/https 网址，避免把相对路径当成站内地址打开。
  try {
    const parsedUrl = new URL(safeUrl);
    return WEBSITE_PROTOCOLS.includes(parsedUrl.protocol) ? parsedUrl.href : '';
  } catch {
    return '';
  }
}
