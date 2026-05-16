import { ElMessage } from 'element-plus';
import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { login, logout, register, type LoginPayload, type RegisterPayload } from '../api/auth';
import { AUTH_TOKEN_STORAGE_KEY } from '../api/http';
import { getCurrentUser, type CurrentUser } from '../api/user';

/**
 * 用户认证状态仓库。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY));
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
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, accessToken);
  }

  /**
   * 清理本地登录态。
   */
  function clearAuth(): void {
    token.value = null;
    user.value = null;
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  }

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
      user.value = await getCurrentUser();
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
    initialize,
    logoutAccount,
  };
});
