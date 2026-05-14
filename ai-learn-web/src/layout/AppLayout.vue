<template>
  <div class="app-layout">
    <aside class="layout-sidebar">
      <div class="brand-block">
        <div class="brand-logo">AI</div>
        <div>
          <h1>AI应用开发学习平台</h1>
          <p>资料，项目，刷题</p>
        </div>
      </div>

      <el-menu
        class="layout-menu"
        :default-active="activeMenu"
        router
      >
        <el-menu-item index="/learning-roadmap">路线和资料</el-menu-item>
        <el-menu-item index="/suggestions-comments">建议评论区</el-menu-item>
        <el-menu-item index="/interview-questions">热门面经</el-menu-item>
        <el-menu-item index="/practice-agent">AI智能刷题</el-menu-item>
      </el-menu>
    </aside>

    <section class="layout-main">
      <header class="layout-header">
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

/**
 * 展示登录注册占位提示。
 */
function showLoginNotice(): void {
  ElMessage.info('登录能力将在 sprint202602 开放');
}
</script>

