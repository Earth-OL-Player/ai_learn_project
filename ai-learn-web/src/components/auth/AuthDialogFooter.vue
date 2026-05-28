<template>
  <div class="auth-dialog-footer modern-auth-footer">
    <el-button round size="large" @click="emit('cancel')">取消</el-button>
    <el-button type="primary" round size="large" :loading="loading" @click="emit('confirm')">
      {{ confirmText }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
// 认证弹窗底部只承载通用取消和确认动作，避免登录、注册模板重复。
interface AuthDialogFooterProps {
  confirmText: string;
  loading: boolean;
}

defineProps<AuthDialogFooterProps>();

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();
</script>
