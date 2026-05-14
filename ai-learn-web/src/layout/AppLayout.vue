<template>
  <div class="app-layout">
    <aside class="layout-sidebar">
      <div class="brand-block">
        <div class="brand-logo">AI</div>
        <div>
          <h1>AI 学习平台</h1>
          <p>路线 · 资料 · 实战</p>
        </div>
      </div>

      <el-menu
        class="layout-menu"
        :default-active="activeMenu"
        router
      >
        <el-menu-item index="/learning-roadmap">AI 学习路线与资料</el-menu-item>
        <el-menu-item index="/suggestions-comments">建议与评论</el-menu-item>
        <el-menu-item index="/interview-questions">面试题大全</el-menu-item>
        <el-menu-item index="/practice-agent">刷题 Agent</el-menu-item>
      </el-menu>
    </aside>

    <section class="layout-main">
      <header class="layout-header">
        <div>
          <span class="header-label">当前页面</span>
          <h2>{{ pageTitle }}</h2>
        </div>
        <el-button type="primary" plain round @click="showLoginNotice">登录 / 注册</el-button>
      </header>

      <main class="layout-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';

const route = useRoute();

// 当前菜单直接跟随路由路径，保证刷新后高亮正确。
const activeMenu = computed(() => route.path);

// 页面标题来自路由元信息，缺省值用于兜底展示。
const pageTitle = computed(() => String(route.meta.title || 'AI 学习路线与资料'));

/**
 * 展示登录注册占位提示。
 */
function showLoginNotice(): void {
  ElMessage.info('登录能力将在 sprint202602 开放');
}
</script>
