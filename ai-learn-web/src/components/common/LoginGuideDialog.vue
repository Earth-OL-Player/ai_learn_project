<template>
  <el-dialog v-model="visible" width="420px" class="login-guide-dialog" destroy-on-close>
    <div class="login-guide-body">
      <div class="login-guide-icon">🔐</div>
      <h3>登录后即可使用该功能</h3>
      <p>登录后可以访问热门面经、AI智能刷题和个人中心，后续学习记录也会自动沉淀。</p>
      <div class="login-guide-actions">
        <el-button @click="visible = false">稍后再说</el-button>
        <el-button type="primary" @click="openLogin">立即登录</el-button>
        <el-button plain type="primary" @click="openRegister">注册账号</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
const visible = defineModel<boolean>({ required: true });
const emit = defineEmits<{
  openLogin: [];
  openRegister: [];
}>();

/**
 * 打开登录弹窗。
 */
function openLogin(): void {
  visible.value = false;
  emit('openLogin');
}

/**
 * 打开注册弹窗。
 */
function openRegister(): void {
  visible.value = false;
  emit('openRegister');
}
</script>
