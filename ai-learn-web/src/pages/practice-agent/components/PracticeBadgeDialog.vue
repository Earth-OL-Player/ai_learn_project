<template>
  <el-dialog
    v-model="dialogVisible"
    class="badge-award-dialog"
    title="获得新勋章"
    width="560px"
    align-center
    :show-close="false"
    :close-on-click-modal="false"
    @closed="emit('closed')"
  >
    <template #header>
      <div class="badge-award-header">
        <span class="badge-award-medal" aria-hidden="true">🏅</span>
        <div>
          <strong>恭喜获得新勋章</strong>
          <p>{{ subtitle }}</p>
        </div>
      </div>
    </template>

    <div class="badge-award-list" :class="{ multiple: badges.length > 1 }">
      <article v-for="badge in badges" :key="badge.ruleCode || badge.id" class="badge-award-item">
        <div class="badge-award-icon">{{ badge.icon }}</div>
        <div class="badge-award-copy">
          <span>{{ badge.categoryName }}</span>
          <strong>{{ badge.name }}</strong>
          <p>{{ badge.description }}</p>
        </div>
      </article>
    </div>

    <template #footer>
      <el-button class="badge-award-confirm" type="primary" round aria-label="关闭勋章弹窗并继续刷题" @click="dialogVisible = false">继续刷题</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { BadgeInfo } from '../../../types/growth';

interface PracticeBadgeDialogProps {
  badges: BadgeInfo[];
  subtitle: string;
  visible: boolean;
}

const props = defineProps<PracticeBadgeDialogProps>();

const emit = defineEmits<{
  closed: [];
  'update:visible': [value: boolean];
}>();

// 弹框显隐使用命名 v-model，避免页面编排层关心 Element Plus 细节。
const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
});
</script>
