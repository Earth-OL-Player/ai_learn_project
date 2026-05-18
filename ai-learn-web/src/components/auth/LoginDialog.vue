<template>
  <el-dialog v-model="visible" width="440px" class="auth-dialog modern-auth-dialog" destroy-on-close align-center>
    <el-form :model="form" label-position="top" class="auth-form" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model.trim="form.username" placeholder="请输入用户名" maxlength="32" size="large" clearable />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" placeholder="请输入密码" type="password" maxlength="64" size="large" show-password />
      </el-form-item>
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
    ElMessage.warning('请输入用户名和密码');
    return;
  }

  submitting.value = true;
  try {
    await authStore.loginByPassword({ ...form });
    visible.value = false;
    form.password = '';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}
</script>
