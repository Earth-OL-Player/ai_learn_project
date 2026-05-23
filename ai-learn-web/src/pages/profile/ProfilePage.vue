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
        <el-button class="profile-edit-button" type="primary" round @click="openProfileDialog">编辑资料</el-button>
      </div>

      <el-descriptions :column="2" border class="profile-descriptions">
        <el-descriptions-item label="用户名">{{ authStore.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ authStore.user?.nickname || '暂未设置' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderText }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ authStore.user?.email || '暂未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formattedCreatedAt }}</el-descriptions-item>
      </el-descriptions>

      <el-dialog v-model="profileDialogVisible" title="编辑资料" width="520px" class="profile-edit-dialog" destroy-on-close align-center>
        <el-form :model="profileForm" label-position="top" class="profile-edit-form" @submit.prevent>
          <el-form-item label="昵称">
            <el-input v-model.trim="profileForm.nickname" maxlength="64" show-word-limit clearable placeholder="请输入昵称" size="large" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="profileForm.gender" clearable placeholder="-" size="large">
              <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-form>

        <template #footer>
          <div class="profile-edit-actions">
            <el-button round size="large" @click="profileDialogVisible = false">取消</el-button>
            <el-button type="primary" round size="large" :loading="savingProfile" @click="saveProfile">保存</el-button>
          </div>
        </template>
      </el-dialog>
    </el-card>

    <GrowthOverviewPanel />
  </section>
</template>

<script setup lang="ts">
import { ElCard } from 'element-plus/es/components/card/index.mjs';
import { ElDescriptions, ElDescriptionsItem } from 'element-plus/es/components/descriptions/index.mjs';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs';
import 'element-plus/es/components/card/style/css';
import 'element-plus/es/components/descriptions/style/css';
import 'element-plus/es/components/select/style/css';
import { computed, reactive, ref, watch } from 'vue';
import { useAuthStore } from '../../stores/auth';
import type { GenderCode } from '../../api/user';
import GrowthOverviewPanel from './components/GrowthOverviewPanel.vue';

const authStore = useAuthStore();
const savingProfile = ref(false);
const profileDialogVisible = ref(false);
const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
] as const;

// 资料表单只维护用户允许本人修改的字段。
const profileForm = reactive<{ nickname: string; gender: GenderCode | '' }>({
  nickname: '',
  gender: '',
});

// 展示名优先使用昵称，未设置时回退用户名。
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'AI 学习者');
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());
const genderText = computed(() => resolveGenderText(authStore.user?.gender || null));

// 注册时间统一展示为本地可读格式。
const formattedCreatedAt = computed(() => {
  if (!authStore.user?.createdAt) {
    return '暂未获取';
  }
  return new Date(authStore.user.createdAt).toLocaleString('zh-CN', { hour12: false });
});

/**
 * 打开资料编辑弹窗。
 */
function openProfileDialog(): void {
  resetProfileForm();
  profileDialogVisible.value = true;
}

/**
 * 重置资料表单。
 */
function resetProfileForm(): void {
  profileForm.nickname = authStore.user?.nickname || '';
  profileForm.gender = authStore.user?.gender || '';
}

/**
 * 保存用户资料。
 */
async function saveProfile(): Promise<void> {
  const nickname = profileForm.nickname.trim();
  if (!nickname || nickname.length > 64) {
    ElMessage.warning('昵称不能为空，且不能超过64位');
    return;
  }

  savingProfile.value = true;
  try {
    await authStore.updateProfile({
      nickname,
      gender: profileForm.gender || null,
    });
    profileDialogVisible.value = false;
    resetProfileForm();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资料保存失败');
  } finally {
    savingProfile.value = false;
  }
}

/**
 * 解析性别展示文案。
 */
function resolveGenderText(gender: GenderCode | null): string {
  if (gender === 'MALE') {
    return '男';
  }
  if (gender === 'FEMALE') {
    return '女';
  }
  return '-';
}

watch(
  () => [authStore.user?.nickname, authStore.user?.gender] as const,
  () => resetProfileForm(),
  { immediate: true },
);
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

.profile-edit-button {
  margin-left: auto;
  font-weight: 700;
}

.profile-edit-form {
  // 弹窗内只保留用户可编辑资料，避免混入系统内部形象逻辑。
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-edit-form :deep(.el-select) {
  width: 100%;
}

.profile-edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 720px) {
  .profile-header {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .profile-edit-button {
    width: 100%;
    margin-left: 0;
  }

  .profile-edit-actions {
    justify-content: stretch;
  }

  .profile-edit-actions .el-button {
    flex: 1;
  }
}
</style>
