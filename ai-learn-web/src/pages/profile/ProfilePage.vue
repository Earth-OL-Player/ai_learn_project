<template>
  <section class="profile-page">
    <el-card shadow="never" class="profile-card">
      <div class="profile-header">
        <el-avatar :size="88" :src="authStore.user?.avatar || undefined" class="profile-avatar">
          {{ avatarText }}
        </el-avatar>
        <div>
          <h2>{{ displayName }}</h2>
          <p>@{{ authStore.user?.username }}</p>
        </div>
      </div>

      <el-descriptions :column="2" border class="profile-descriptions">
        <el-descriptions-item label="用户名">{{ authStore.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ authStore.user?.nickname || '暂未设置' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ authStore.user?.email || '暂未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formattedCreatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <GrowthOverviewPanel />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '../../stores/auth';
import GrowthOverviewPanel from './components/GrowthOverviewPanel.vue';

const authStore = useAuthStore();

// 展示名优先使用昵称，未设置时回退用户名。
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'AI 学习者');
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());

// 注册时间统一展示为本地可读格式。
const formattedCreatedAt = computed(() => {
  if (!authStore.user?.createdAt) {
    return '暂未获取';
  }
  return new Date(authStore.user.createdAt).toLocaleString('zh-CN', { hour12: false });
});
</script>

<style scoped lang="scss">
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-card {
  border: 1px solid #edf2f7;
  border-radius: 18px;
}

.profile-header {
  // 基础资料卡只保留用户身份信息，成长信息统一放入下方成长概览。
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 22px;
  padding: 18px 10px 6px;
}

.profile-header h2 {
  margin: 0;
  color: #1f2a44;
}

.profile-header p {
  margin: 6px 0 0;
  color: #667085;
}
</style>
