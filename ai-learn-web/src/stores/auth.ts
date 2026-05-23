import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { login, logout, register, type LoginPayload, type RegisterPayload } from '../api/auth';
import { getCurrentUser, updateCurrentProfile, type CurrentUser, type UpdateProfilePayload } from '../api/user';
import {
  AUTH_TOKEN_CLEARED_EVENT,
  clearStoredAccessToken,
  getStoredAccessToken,
  setStoredAccessToken,
} from '../utils/authTokenStorage';

/**
 * 用户认证状态仓库。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getStoredAccessToken());
  const user = ref<CurrentUser | null>(null);
  const initialized = ref(false);

  // 是否已登录由 token 和用户信息共同确认。
  const isLoggedIn = computed(() => Boolean(token.value && user.value));
  const isSuperAdmin = computed(() => Boolean(isLoggedIn.value && user.value?.superAdmin));

  /**
   * 保存登录成功后的令牌和用户信息。
   */
  function setAuth(accessToken: string, currentUser: CurrentUser): void {
    token.value = accessToken;
    user.value = currentUser;
    setStoredAccessToken(accessToken);
  }

  /**
   * 清理本地登录态。
   */
  function clearAuth(): void {
    token.value = null;
    user.value = null;
    clearStoredAccessToken();
  }

  /**
   * 监听接口层发现的认证失效事件。
   */
  window.addEventListener(AUTH_TOKEN_CLEARED_EVENT, () => {
    token.value = null;
    user.value = null;
  });

  /**
   * 执行登录。
   */
  async function loginByPassword(payload: LoginPayload): Promise<void> {
    const result = await login(payload);
    setAuth(result.accessToken, result.user);
    ElMessage.success('登录成功');
  }

  /**
   * 执行注册。
   */
  async function registerAccount(payload: RegisterPayload): Promise<void> {
    const result = await register(payload);
    setAuth(result.accessToken, result.user);
    ElMessage.success('注册成功');
  }

  /**
   * 刷新当前用户资料。
   */
  async function refreshCurrentUser(): Promise<void> {
    user.value = await getCurrentUser();
  }

  /**
   * 更新当前用户资料。
   */
  async function updateProfile(payload: UpdateProfilePayload): Promise<void> {
    user.value = await updateCurrentProfile(payload);
    ElMessage.success('资料已保存');
  }

  /**
   * 初始化并恢复登录态。
   */
  async function initialize(): Promise<void> {
    if (initialized.value) {
      return;
    }
    initialized.value = true;
    if (!token.value) {
      return;
    }

    try {
      await refreshCurrentUser();
    } catch {
      clearAuth();
    }
  }

  /**
   * 退出登录。
   */
  async function logoutAccount(): Promise<void> {
    try {
      if (token.value) {
        await logout();
      }
    } finally {
      clearAuth();
      ElMessage.success('已退出登录');
    }
  }

  return {
    token,
    user,
    initialized,
    isLoggedIn,
    isSuperAdmin,
    setAuth,
    clearAuth,
    loginByPassword,
    registerAccount,
    refreshCurrentUser,
    updateProfile,
    initialize,
    logoutAccount,
  };
});
