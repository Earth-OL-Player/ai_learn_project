<template>
  <div class="app-layout">
    <header class="layout-header">
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
      >
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item index="/learning-roadmap">路线和资料</el-menu-item>
        <el-menu-item index="/practice-agent">AI智能刷题</el-menu-item>
        <el-menu-item index="/interview-questions">热门面试题</el-menu-item>
        <el-menu-item index="/suggestions-comments">建议评论区</el-menu-item>
      </el-menu>

      <!-- 顶部右侧继续承载登录注册和用户操作入口。 -->
      <div class="header-action-area">
        <div v-if="authStore.isLoggedIn" class="header-user">
          <el-avatar :size="32" :src="authStore.user?.avatar || undefined">{{ avatarText }}</el-avatar>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="user-dropdown-button" type="button">
              {{ displayName }}
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
          <el-button plain round @click="showRegisterDialog = true">注册</el-button>
          <el-button type="primary" plain round @click="showLoginDialog = true">登录</el-button>
        </div>
      </div>
    </header>

    <section class="layout-main">
      <main class="layout-content">
        <RouterView v-slot="{ Component, route }">
          <KeepAlive>
            <component :is="Component" v-if="route.meta.keepAlive" />
          </KeepAlive>
          <component :is="Component" v-if="!route.meta.keepAlive" />
        </RouterView>
      </main>
    </section>

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
import { RouterView, useRoute, useRouter } from 'vue-router';
import LoginDialog from '../components/auth/LoginDialog.vue';
import RegisterDialog from '../components/auth/RegisterDialog.vue';
import LoginGuideDialog from '../components/common/LoginGuideDialog.vue';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const showLoginDialog = ref(false);
const showRegisterDialog = ref(false);
const showLoginGuideDialog = ref(false);

// 当前菜单直接跟随路由路径，保证刷新后高亮正确。
const activeMenu = computed(() => route.path);
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'AI 学习者');
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());

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
