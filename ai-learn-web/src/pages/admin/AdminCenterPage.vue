<template>
  <section class="admin-center-page">
    <aside class="admin-sidebar" aria-label="管理者中心导航">
      <div class="admin-sidebar-header">
        <span class="admin-sidebar-icon">管</span>
        <div>
          <h2>管理者中心</h2>
        </div>
      </div>

      <!-- 左侧菜单仅承载当前迭代已规划的管理功能入口。 -->
      <el-menu class="admin-menu" :default-active="route.path" router aria-label="管理功能导航">
        <el-menu-item index="/admin/users">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/system-question-bank">
          <span>系统题库管理</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="admin-content" aria-label="管理内容区">
      <RouterView />
    </main>
  </section>
</template>

<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router';

const route = useRoute();
</script>
