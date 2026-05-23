<template>
  <el-dialog v-model="visible" title="登录账号" aria-label="登录账号" width="440px" class="auth-dialog modern-auth-dialog" destroy-on-close align-center>
    <el-form :model="form" label-position="top" class="auth-form" @submit.prevent @keyup.enter="submitLogin">
      <h2 class="sr-only">登录账号</h2>
      <el-form-item label="用户名">
        <el-input
          v-model.trim="form.username"
          aria-label="用户名"
          autocomplete="username"
          placeholder="请输入用户名"
          maxlength="32"
          size="large"
          clearable
        />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="form.password"
          aria-label="密码"
          autocomplete="current-password"
          placeholder="请输入密码"
          type="password"
          maxlength="64"
          size="large"
          show-password
        />
      </el-form-item>
      <p v-if="formError" class="auth-form-error" role="alert">{{ formError }}</p>
    </el-form>

    <template #footer>
      <div class="auth-dialog-footer modern-auth-footer">
        <el-button round size="large" @click="visible = false">取消</el-button>
        <el-button type="primary" round size="large" :loading="submitting" @click="submitLogin">立即登录</el-button>
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

// 登录表单只保留本期必需字段。
const form = reactive({
  username: '',
  password: '',
});

/**
 * 提交登录表单。
 */
async function submitLogin(): Promise<void> {
  if (!form.username || !form.password) {
    formError.value = '请输入用户名和密码';
    ElMessage.warning(formError.value);
    return;
  }

  submitting.value = true;
  formError.value = '';
  try {
    await authStore.loginByPassword({ ...form });
    visible.value = false;
    form.password = '';
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '登录失败，请稍后重试';
    ElMessage.error(formError.value);
  } finally {
    submitting.value = false;
  }
}
</script>
