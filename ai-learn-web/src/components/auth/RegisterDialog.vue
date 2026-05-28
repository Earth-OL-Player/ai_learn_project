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
            :maxlength="USER_PROFILE_LIMITS.usernameMaxLength"
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
            :maxlength="USER_PROFILE_LIMITS.passwordMaxLength"
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
          :maxlength="USER_PROFILE_LIMITS.nicknameMaxLength"
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
          :maxlength="USER_PROFILE_LIMITS.emailMaxLength"
          size="large"
          clearable
        />
      </el-form-item>
      <p v-if="formError" class="auth-form-error" role="alert">{{ formError }}</p>
    </el-form>

    <template #footer>
      <AuthDialogFooter
        confirm-text="注册并登录"
        :loading="submitting"
        @cancel="visible = false"
        @confirm="submitRegister"
      />
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { reactive, ref } from 'vue';
import { useAuthStore } from '../../stores/auth';
import { resolveErrorMessage } from '../../utils/errorMessage';
import {
  isValidEmail,
  isValidNickname,
  isValidPasswordLength,
  isValidUsername,
  USER_PROFILE_LIMITS,
  USER_PROFILE_MESSAGES,
} from '../../utils/userProfileValidation';
import AuthDialogFooter from './AuthDialogFooter.vue';

const visible = defineModel<boolean>({ required: true });
const authStore = useAuthStore();
const submitting = ref(false);
const formError = ref('');

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
  if (!isValidUsername(form.username)) {
    return rejectForm(USER_PROFILE_MESSAGES.usernameInvalid);
  }
  if (!isValidPasswordLength(form.password)) {
    return rejectForm(USER_PROFILE_MESSAGES.passwordInvalid);
  }
  if (!isValidNickname(form.nickname)) {
    return rejectForm(USER_PROFILE_MESSAGES.nicknameInvalid);
  }
  if (!isValidEmail(form.email)) {
    return rejectForm(USER_PROFILE_MESSAGES.emailInvalid);
  }
  formError.value = '';
  return true;
}

/**
 * 提示注册表单错误。
 *
 * @param message 错误提示
 */
function rejectForm(message: string): false {
  formError.value = message;
  ElMessage.warning(message);
  return false;
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
    formError.value = resolveErrorMessage(error, '注册失败，请稍后重试');
    ElMessage.error(formError.value);
  } finally {
    submitting.value = false;
  }
}
</script>
