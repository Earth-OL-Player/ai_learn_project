<template>
  <el-dialog v-model="visible" title="注册账号" width="460px" class="auth-dialog" destroy-on-close>
    <el-form :model="form" label-position="top" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model.trim="form.username" placeholder="3-32位字母、数字、下划线" maxlength="32" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" placeholder="8-64位密码" type="password" maxlength="64" show-password />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model.trim="form.nickname" placeholder="必填，1-64位且不可重复" maxlength="64" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model.trim="form.email" placeholder="必填，例如 demo@example.com，且不可重复" maxlength="128" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="auth-dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRegister">注册并登录</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { reactive, ref } from 'vue';
import { useAuthStore } from '../../stores/auth';

const visible = defineModel<boolean>({ required: true });
const authStore = useAuthStore();
const submitting = ref(false);
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
    ElMessage.warning('用户名仅支持3到32位字母、数字和下划线');
    return false;
  }
  if (form.password.length < 8 || form.password.length > 64) {
    ElMessage.warning('密码长度需为8到64位');
    return false;
  }
  if (!form.nickname || form.nickname.length > 64) {
    ElMessage.warning('昵称不能为空，且不能超过64位');
    return false;
  }
  if (!emailPattern.test(form.email)) {
    ElMessage.warning('请输入正确的邮箱地址');
    return false;
  }
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
    ElMessage.error(error instanceof Error ? error.message : '注册失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}
</script>
