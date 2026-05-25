import { AUTH_TOKEN_STORAGE_KEY } from '../constants/api';

export const AUTH_TOKEN_CLEARED_EVENT = 'ai-learn-auth-token-cleared';

/**
 * 获取浏览器级访问令牌。
 */
export function getStoredAccessToken(): string | null {
  const persistentToken = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
  if (persistentToken) {
    clearLegacySessionStorageToken();
    return persistentToken;
  }

  // 兼容历史版本登录态，避免升级后当前窗口需要重新登录。
  const legacySessionToken = sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
  if (legacySessionToken) {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, legacySessionToken);
    clearLegacySessionStorageToken();
  }
  return legacySessionToken;
}

/**
 * 保存浏览器级访问令牌。
 */
export function setStoredAccessToken(accessToken: string): void {
  localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, accessToken);
  clearLegacySessionStorageToken();
}

/**
 * 清理浏览器侧访问令牌。
 */
export function clearStoredAccessToken(): void {
  localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  clearLegacySessionStorageToken();
  window.dispatchEvent(new Event(AUTH_TOKEN_CLEARED_EVENT));
}

/**
 * 清理历史版本保存在 sessionStorage 的访问令牌。
 */
function clearLegacySessionStorageToken(): void {
  sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
}
