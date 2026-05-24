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

      <el-descriptions :column="profileDescriptionColumn" border class="profile-descriptions">
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

    <el-card shadow="never" class="profile-model-card">
      <div class="profile-model-header">
        <div>
          <h3>AI刷题模型</h3>
        </div>
      </div>

      <ModelEntitlementSummary :status="modelEntitlementStatus" @authorize="handleModelAuthorize" />

      <div class="profile-redeem-panel">
        <el-input
          v-model.trim="redeemForm.code"
          maxlength="32"
          clearable
          size="large"
          placeholder="请输入授权码"
          aria-label="授权码"
          @keyup.enter="redeemCode"
        />
        <el-button type="primary" round size="large" :loading="redeeming" @click="redeemCode">授权</el-button>
      </div>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { fetchModelEntitlementStatus, redeemModelCode, type ModelEntitlementStatus } from '../../api/modelEntitlements';
import { useAuthStore } from '../../stores/auth';
import ModelEntitlementSummary from '../../components/model/ModelEntitlementSummary.vue';
import type { GenderCode } from '../../api/user';
import GrowthOverviewPanel from './components/GrowthOverviewPanel.vue';
import { openModelAuthorization } from '../../utils/modelAuthorization';

const PROFILE_MOBILE_QUERY = '(max-width: 720px)';
const authStore = useAuthStore();
const savingProfile = ref(false);
const redeeming = ref(false);
const profileDialogVisible = ref(false);
const isProfileMobile = ref(false);
const modelEntitlementStatus = ref<ModelEntitlementStatus | null>(null);
let profileMediaQuery: MediaQueryList | null = null;
const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
] as const;

// 资料表单只维护用户允许本人修改的字段。
const profileForm = reactive<{ nickname: string; gender: GenderCode | '' }>({
  nickname: '',
  gender: '',
});
const redeemForm = reactive({ code: '' });

// 展示名优先使用昵称，未设置时回退用户名。
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'AI 学习者');
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());
const genderText = computed(() => resolveGenderText(authStore.user?.gender || null));
const profileDescriptionColumn = computed(() => (isProfileMobile.value ? 1 : 2));

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
 * 加载模型权益信息。
 */
async function loadModelEntitlementStatus(): Promise<void> {
  modelEntitlementStatus.value = await fetchModelEntitlementStatus();
}

/**
 * 打开模型授权入口。
 */
async function handleModelAuthorize(): Promise<void> {
  await openModelAuthorization(modelEntitlementStatus.value);
}

/**
 * 授权模型权益授权码。
 */
async function redeemCode(): Promise<void> {
  const code = redeemForm.code.trim();
  if (!code) {
    ElMessage.warning('请输入授权码');
    return;
  }
  redeeming.value = true;
  try {
    const result = await redeemModelCode(code);
    modelEntitlementStatus.value = result.entitlement;
    redeemForm.code = '';
    ElMessage.success(result.message);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '授权失败');
  } finally {
    redeeming.value = false;
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

/**
 * 同步个人资料表格列数断点。
 *
 * @param event 媒体查询变化事件
 */
function syncProfileViewport(event?: MediaQueryListEvent): void {
  isProfileMobile.value = event ? event.matches : Boolean(profileMediaQuery?.matches);
}

watch(
  () => [authStore.user?.nickname, authStore.user?.gender] as const,
  () => resetProfileForm(),
  { immediate: true },
);

onMounted(() => {
  // 资料描述组件需要真实列数，避免手机端两列表格撑破屏幕。
  profileMediaQuery = window.matchMedia(PROFILE_MOBILE_QUERY);
  syncProfileViewport();
  profileMediaQuery.addEventListener('change', syncProfileViewport);
  loadModelEntitlementStatus().catch((error: unknown) => {
    ElMessage.error(error instanceof Error ? error.message : '模型权益加载失败');
  });
});

onBeforeUnmount(() => {
  profileMediaQuery?.removeEventListener('change', syncProfileViewport);
  profileMediaQuery = null;
});
</script>

<style scoped lang="scss">
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-card {
  border: 1px solid var(--color-border);
  border-radius: 18px;
}

.profile-model-card {
  border: 1px solid var(--color-border);
  border-radius: 18px;
}

.profile-model-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.profile-model-header h3 {
  margin: 0;
  color: var(--color-heading);
  font-size: 20px;
}

.profile-redeem-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  margin-top: 18px;
}

.profile-redeem-panel .el-button {
  min-width: 112px;
  font-weight: 800;
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
  color: var(--color-heading);
}

.profile-header p {
  margin: 6px 0 0;
  color: var(--color-muted);
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
  .profile-page {
    gap: 14px;
  }

  .profile-card {
    border-radius: 18px;
  }

  .profile-header {
    align-items: flex-start;
    flex-wrap: wrap;
    padding: 12px 0 4px;
  }

  .profile-header :deep(.el-avatar) {
    width: 64px !important;
    height: 64px !important;
    font-size: 22px;
  }

  .profile-header h2 {
    font-size: 23px;
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
    min-height: 44px;
  }

  .profile-redeem-panel {
    grid-template-columns: 1fr;
  }

  .profile-redeem-panel .el-button {
    width: 100%;
    min-height: 44px;
  }
}
</style>
