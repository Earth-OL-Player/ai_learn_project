import { AUTH_TOKEN_STORAGE_KEY } from '../constants/api';

export const AUTH_TOKEN_CLEARED_EVENT = 'ai-learn-auth-token-cleared';

/**
 * 获取会话级访问令牌。
 */
export function getStoredAccessToken(): string | null {
  clearLegacyLocalStorageToken();
  return sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
}

/**
 * 保存会话级访问令牌。
 */
export function setStoredAccessToken(accessToken: string): void {
  sessionStorage.setItem(AUTH_TOKEN_STORAGE_KEY, accessToken);
  clearLegacyLocalStorageToken();
}

/**
 * 清理浏览器侧访问令牌。
 */
export function clearStoredAccessToken(): void {
  sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  clearLegacyLocalStorageToken();
  window.dispatchEvent(new Event(AUTH_TOKEN_CLEARED_EVENT));
}

/**
 * 清理历史版本保存在 localStorage 的访问令牌。
 */
function clearLegacyLocalStorageToken(): void {
  localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
}
