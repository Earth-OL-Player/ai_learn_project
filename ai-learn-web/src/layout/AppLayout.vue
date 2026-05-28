<template>
  <div class="app-layout">
    <a class="skip-link" href="#main-content">跳到正文</a>

    <header class="layout-header" role="banner">
      <!-- 顶部左侧保留平台品牌信息，替代原来的侧边栏品牌区。 -->
      <div class="brand-block">
        <div class="brand-logo">AI</div>
        <div class="brand-title-row">
          <h1>Agent学习平台</h1>
          <span>by 地球OL初级玩家</span>
        </div>
      </div>

      <!-- 主导航改为顶部横向菜单，方便用户在页面顶部快速切换模块。 -->
      <el-menu
        class="layout-menu"
        :default-active="activeMenu"
        mode="horizontal"
        router
        aria-label="主导航"
      >
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item index="/learning-roadmap">路线和资料</el-menu-item>
        <el-menu-item index="/practice-agent">AI智能刷题</el-menu-item>
        <el-menu-item index="/interview-questions">热门面试题</el-menu-item>
        <el-menu-item index="/suggestions-comments">建议评论区</el-menu-item>
      </el-menu>

      <!-- 顶部右侧继续承载登录注册和用户操作入口。 -->
      <div class="header-action-area">
        <button
          :class="['theme-toggle-button', { 'is-dark': isDarkMode }]"
          type="button"
          :aria-label="themeToggleLabel"
          :aria-pressed="isDarkMode"
          @click="toggleThemeMode"
        >
          <span class="theme-toggle-icon-wrap" aria-hidden="true">
            <span :class="['theme-toggle-icon', isDarkMode ? 'theme-toggle-moon' : 'theme-toggle-sun']"></span>
          </span>
        </button>
        <div v-if="authStore.isLoggedIn" class="header-user">
          <el-avatar :size="32" :src="authStore.user?.avatar || undefined">{{ avatarText }}</el-avatar>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <button
              class="user-dropdown-button"
              type="button"
              aria-haspopup="menu"
              :aria-label="`${displayName} 的用户菜单`"
            >
              <span class="user-dropdown-name">{{ displayName }}</span>
              <span class="user-dropdown-arrow" aria-hidden="true"></span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="authStore.isSuperAdmin" command="admin">管理者中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div v-else class="header-auth-actions">
          <el-button plain round aria-label="打开注册弹窗" @click="showRegisterDialog = true">注册</el-button>
          <el-button type="primary" plain round aria-label="打开登录弹窗" @click="showLoginDialog = true">登录</el-button>
        </div>
      </div>
    </header>

    <section class="layout-main">
      <main id="main-content" class="layout-content" tabindex="-1">
        <RouterView v-slot="{ Component, route }">
          <KeepAlive>
            <component :is="Component" v-if="route.meta.keepAlive" />
          </KeepAlive>
          <component :is="Component" v-if="!route.meta.keepAlive" />
        </RouterView>
      </main>
    </section>

    <nav class="mobile-bottom-nav" aria-label="手机主导航">
      <RouterLink
        v-for="item in mobileNavItems"
        :key="item.path"
        :to="item.path"
        :class="['mobile-bottom-nav-item', { 'is-active': isMobileNavActive(item.path) }]"
        :aria-current="isMobileNavActive(item.path) ? 'page' : undefined"
      >
        <span class="mobile-bottom-nav-key" aria-hidden="true">{{ item.key }}</span>
        <span class="mobile-bottom-nav-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <LoginDialog v-model="showLoginDialog" />
    <RegisterDialog v-model="showRegisterDialog" />
    <LoginGuideDialog
      v-model="showLoginGuideDialog"
      @open-login="showLoginDialog = true"
      @open-register="showRegisterDialog = true"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import LoginDialog from '../components/auth/LoginDialog.vue';
import RegisterDialog from '../components/auth/RegisterDialog.vue';
import LoginGuideDialog from '../components/common/LoginGuideDialog.vue';
import { useAuthStore } from '../stores/auth';
import { resolveAvatarText, resolveUserDisplayName } from '../utils/userDisplay';

type ThemeMode = 'light' | 'dark';

interface MobileNavItem {
  path: string;
  label: string;
  key: string;
}

const THEME_STORAGE_KEY = 'ai-learn-theme-mode';
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const showLoginDialog = ref(false);
const showRegisterDialog = ref(false);
const showLoginGuideDialog = ref(false);
const themeMode = ref<ThemeMode>(resolveInitialThemeMode());

// 手机端底部导航只承载最高频的五个公开主功能入口。
const mobileNavItems: MobileNavItem[] = [
  { path: '/home', label: '首页', key: '首' },
  { path: '/learning-roadmap', label: '路线', key: '路' },
  { path: '/practice-agent', label: '刷题', key: '练' },
  { path: '/interview-questions', label: '面试', key: '题' },
  { path: '/suggestions-comments', label: '互动', key: '评' },
];

// 当前菜单直接跟随路由路径，保证刷新后高亮正确。
const activeMenu = computed(() => route.path);
const displayName = computed(() => resolveUserDisplayName(authStore.user));
const avatarText = computed(() => resolveAvatarText(displayName.value));
const isDarkMode = computed(() => themeMode.value === 'dark');
const themeToggleLabel = computed(() => (isDarkMode.value ? '切换到白天模式' : '切换到黑夜模式'));

watch(
  () => route.query.loginGuide,
  (value) => {
    if (value === '1') {
      showLoginGuideDialog.value = true;
      router.replace({ path: route.path, query: { ...route.query, loginGuide: undefined } });
    }
  },
  { immediate: true },
);

watch(
  themeMode,
  (value) => {
    applyThemeMode(value);
    persistThemeMode(value);
  },
  { immediate: true },
);

/**
 * 判断手机底部导航是否命中当前路由。
 *
 * @param path 导航入口路径
 */
function isMobileNavActive(path: string): boolean {
  return route.path === path;
}

/**
 * 获取用户保存的主题；默认保持白天模式。
 */
function resolveInitialThemeMode(): ThemeMode {
  if (typeof window === 'undefined') {
    return 'light';
  }

  try {
    const storedThemeMode = window.localStorage.getItem(THEME_STORAGE_KEY);
    return storedThemeMode === 'dark' ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

/**
 * 将主题模式同步到根节点，供全局样式令牌生效。
 */
function applyThemeMode(value: ThemeMode): void {
  if (typeof document === 'undefined') {
    return;
  }

  document.documentElement.dataset.theme = value;
  document.documentElement.style.setProperty('color-scheme', value);
}

/**
 * 持久化用户主题选择，刷新后继续使用上次模式。
 */
function persistThemeMode(value: ThemeMode): void {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, value);
  } catch {
    // 浏览器禁用本地存储时，仅保留当前会话内主题切换效果。
    return;
  }
}

/**
 * 切换白天模式和黑夜模式。
 */
function toggleThemeMode(): void {
  themeMode.value = isDarkMode.value ? 'light' : 'dark';
}

/**
 * 处理用户下拉菜单命令。
 */
async function handleUserCommand(command: string): Promise<void> {
  if (command === 'profile') {
    await router.push('/profile');
    return;
  }
  if (command === 'admin') {
    await router.push('/admin/users');
    return;
  }
  if (command === 'logout') {
    await authStore.logoutAccount();
    await router.push('/home');
  }
}
</script>
