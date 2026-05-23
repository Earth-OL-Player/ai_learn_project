<template>
  <el-dialog v-model="visible" title="注册账号" aria-label="注册账号" width="500px" class="auth-dialog modern-auth-dialog" destroy-on-close align-center>
    <el-form :model="form" label-position="top" class="auth-form" @submit.prevent @keyup.enter="submitRegister">
      <h2 class="sr-only">注册账号</h2>
      <div class="auth-form-grid">
        <el-form-item label="用户名">
          <el-input
            v-model.trim="form.username"
            aria-label="用户名"
            autocomplete="username"
            placeholder="3-32位字母、数字、下划线"
            maxlength="32"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            aria-label="密码"
            autocomplete="new-password"
            placeholder="8-64位密码"
            type="password"
            maxlength="64"
            size="large"
            show-password
          />
        </el-form-item>
      </div>
      <el-form-item label="昵称">
        <el-input
          v-model.trim="form.nickname"
          aria-label="昵称"
          autocomplete="nickname"
          placeholder="必填，1-64位且不可重复"
          maxlength="64"
          size="large"
          clearable
        />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input
          v-model.trim="form.email"
          aria-label="邮箱"
          autocomplete="email"
          placeholder="必填，例如 demo@example.com，且不可重复"
          maxlength="128"
          size="large"
          clearable
        />
      </el-form-item>
      <p v-if="formError" class="auth-form-error" role="alert">{{ formError }}</p>
    </el-form>

    <template #footer>
      <div class="auth-dialog-footer modern-auth-footer">
        <el-button round size="large" @click="visible = false">取消</el-button>
        <el-button type="primary" round size="large" :loading="submitting" @click="submitRegister">注册并登录</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { reactive, ref } from 'vue';
import { useAuthStore } from '../../stores/auth';

const visible = defineModel<boolean>({ required: true });
const authStore = useAuthStore();
const submitting = ref(false);
const formError = ref('');
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// 注册表单与后端接口字段保持一致。
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
});

/**
 * 校验注册表单。
 */
function validateForm(): boolean {
  if (!/^[A-Za-z0-9_]{3,32}$/.test(form.username)) {
    formError.value = '用户名仅支持3到32位字母、数字和下划线';
    ElMessage.warning(formError.value);
    return false;
  }
  if (form.password.length < 8 || form.password.length > 64) {
    formError.value = '密码长度需为8到64位';
    ElMessage.warning(formError.value);
    return false;
  }
  if (!form.nickname || form.nickname.length > 64) {
    formError.value = '昵称不能为空，且不能超过64位';
    ElMessage.warning(formError.value);
    return false;
  }
  if (!emailPattern.test(form.email)) {
    formError.value = '请输入正确的邮箱地址';
    ElMessage.warning(formError.value);
    return false;
  }
  formError.value = '';
  return true;
}

/**
 * 提交注册表单。
 */
async function submitRegister(): Promise<void> {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  try {
    await authStore.registerAccount({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      email: form.email,
    });
    visible.value = false;
    form.password = '';
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '注册失败，请稍后重试';
    ElMessage.error(formError.value);
  } finally {
    submitting.value = false;
  }
}
</script>
