<template>
  <section class="admin-user-page">
    <div class="admin-page-hero user-manage-hero">
      <div>
        <h2>用户管理</h2>
        <p>统一维护平台账号、超级管理员权限和系统用户容量上限。</p>
      </div>
      <el-button type="primary" round @click="openCreateDialog">新增用户</el-button>
    </div>

    <el-card shadow="never" class="user-limit-card">
      <div class="user-limit-copy">
        <span>最大用户数限制</span>
        <strong>{{ limitInfo.currentUsers }} / {{ limitInfo.maxUsers }}</strong>
        <small>达到上限后，注册页将提示用户等待管理员升级服务器并扩容。</small>
      </div>
      <div class="user-limit-action">
        <el-input-number v-model="limitForm.maxUsers" :min="1" :max="1000000" :step="10" />
        <el-button type="primary" round :loading="savingLimit" @click="saveUserLimit">保存限制</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="admin-filter-card">
      <el-form :model="filters" label-position="top" class="admin-user-filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="搜索用户名、昵称或邮箱" @keyup.enter="searchUsers" />
        </el-form-item>
        <el-form-item class="admin-filter-actions">
          <el-button type="primary" round :loading="loading" @click="searchUsers">查询</el-button>
          <el-button round @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="admin-table-card">
      <el-table v-loading="loading" :data="users" row-key="id">
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="admin-user-cell">
              <el-avatar :size="34" :src="row.avatar || undefined">{{ avatarText(row) }}</el-avatar>
              <div>
                <strong>{{ row.nickname }}</strong>
                <span>@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag :type="row.superAdmin ? 'success' : 'info'" round>
              {{ row.superAdmin ? '超级管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="experience" label="经验" width="100" />
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="table-action-row">
              <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.pageNo"
        layout="prev, pager, next, total"
        :total="page.total"
        :page-size="page.pageSize"
        @current-change="loadUsers"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="620px" class="admin-user-dialog">
      <el-form :model="form" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="用户名">
            <el-input v-model.trim="form.username" maxlength="32" placeholder="3-32位字母、数字、下划线" />
          </el-form-item>
          <el-form-item :label="editingId ? '新密码（可空）' : '密码'">
            <el-input v-model="form.password" type="password" maxlength="64" show-password placeholder="8-64位密码" />
          </el-form-item>
        </div>
        <div class="dialog-grid">
          <el-form-item label="昵称">
            <el-input v-model.trim="form.nickname" maxlength="64" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model.trim="form.email" maxlength="128" placeholder="demo@example.com" />
          </el-form-item>
        </div>
        <el-form-item label="头像地址（可空）">
          <el-input v-model.trim="form.avatar" maxlength="255" placeholder="使用 HTTPS 图片地址，留空则展示文字头像" />
        </el-form-item>
        <el-form-item label="权限">
          <el-switch v-model="form.superAdmin" active-text="超级管理员" inactive-text="普通用户" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import { onMounted, reactive, ref } from 'vue';
import {
  createAdminUser,
  deleteAdminUser,
  fetchAdminUsers,
  fetchUserLimit,
  updateAdminUser,
  updateUserLimit,
} from '../../api/adminUsers';
import type { AdminUserItem, AdminUserPayload, UserLimitInfo } from '../../types/admin-user';

const PAGE_SIZE = 10;
const loading = ref(false);
const saving = ref(false);
const savingLimit = ref(false);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const users = ref<AdminUserItem[]>([]);
const page = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const filters = reactive({ keyword: '' });
const limitInfo = reactive<UserLimitInfo>({ maxUsers: 10000, currentUsers: 0 });
const limitForm = reactive({ maxUsers: 10000 });
const form = reactive<AdminUserPayload>(buildEmptyForm());

/**
 * 构建空用户表单。
 */
function buildEmptyForm(): AdminUserPayload {
  return { username: '', password: '', nickname: '', email: '', avatar: '', superAdmin: false };
}

/**
 * 加载用户容量限制。
 */
async function loadUserLimit(): Promise<void> {
  const result = await fetchUserLimit();
  Object.assign(limitInfo, result);
  limitForm.maxUsers = result.maxUsers;
}

/**
 * 保存用户容量限制。
 */
async function saveUserLimit(): Promise<void> {
  savingLimit.value = true;
  try {
    const result = await updateUserLimit(limitForm.maxUsers);
    Object.assign(limitInfo, result);
    ElMessage.success('最大用户数限制已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存限制失败');
  } finally {
    savingLimit.value = false;
  }
}

/**
 * 加载用户列表。
 */
async function loadUsers(): Promise<void> {
  loading.value = true;
  try {
    const result = await fetchAdminUsers({ pageNo: page.pageNo, pageSize: page.pageSize, ...filters });
    users.value = result.records;
    page.total = result.total;
  } finally {
    loading.value = false;
  }
}

/**
 * 按条件查询用户。
 */
async function searchUsers(): Promise<void> {
  page.pageNo = 1;
  await loadUsers();
}

/**
 * 重置筛选条件。
 */
async function resetFilters(): Promise<void> {
  filters.keyword = '';
  await searchUsers();
}

/**
 * 打开新增弹窗。
 */
function openCreateDialog(): void {
  editingId.value = null;
  Object.assign(form, buildEmptyForm());
  dialogVisible.value = true;
}

/**
 * 打开编辑弹窗。
 */
function openEditDialog(row: AdminUserItem): void {
  editingId.value = row.id;
  Object.assign(form, {
    username: row.username,
    password: '',
    nickname: row.nickname,
    email: row.email,
    avatar: row.avatar || '',
    superAdmin: row.superAdmin,
  });
  dialogVisible.value = true;
}

/**
 * 保存用户。
 */
async function saveUser(): Promise<void> {
  if (!validateUserForm()) {
    return;
  }
  saving.value = true;
  try {
    const payload = normalizePayload();
    if (editingId.value) {
      await updateAdminUser(editingId.value, payload);
      ElMessage.success('用户已更新');
    } else {
      await createAdminUser(payload);
      ElMessage.success('用户已新增');
    }
    dialogVisible.value = false;
    await Promise.all([loadUsers(), loadUserLimit()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存用户失败');
  } finally {
    saving.value = false;
  }
}

/**
 * 删除用户。
 */
async function handleDelete(row: AdminUserItem): Promise<void> {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」吗？`, '删除确认', { type: 'warning' });
  await deleteAdminUser(row.id);
  ElMessage.success('用户已删除');
  await Promise.all([loadUsers(), loadUserLimit()]);
}

/**
 * 校验用户表单。
 */
function validateUserForm(): boolean {
  if (!/^[A-Za-z0-9_]{3,32}$/.test(form.username)) {
    ElMessage.warning('用户名仅支持3到32位字母、数字和下划线');
    return false;
  }
  if (!editingId.value && (!form.password || form.password.length < 8)) {
    ElMessage.warning('新增用户密码长度需为8到64位');
    return false;
  }
  if (form.password && (form.password.length < 8 || form.password.length > 64)) {
    ElMessage.warning('密码长度需为8到64位');
    return false;
  }
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空');
    return false;
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    ElMessage.warning('请输入正确的邮箱地址');
    return false;
  }
  return true;
}

/**
 * 规整保存载荷。
 */
function normalizePayload(): AdminUserPayload {
  return {
    username: form.username.trim(),
    password: form.password?.trim() || undefined,
    nickname: form.nickname.trim(),
    email: form.email.trim(),
    avatar: form.avatar?.trim() || null,
    superAdmin: form.superAdmin,
  };
}

/**
 * 生成头像默认文字。
 */
function avatarText(row: AdminUserItem): string {
  return (row.nickname || row.username).slice(0, 1).toUpperCase();
}

/**
 * 格式化时间。
 */
function formatTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadUserLimit()]);
});
</script>
