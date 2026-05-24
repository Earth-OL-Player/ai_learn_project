<template>
  <div v-if="status" :class="['model-entitlement-summary', { 'is-compact': compact }]">
    <div class="model-entitlement-item">
      <span>刷题模型</span>
      <strong>{{ status.modelName }}</strong>
    </div>

    <div v-if="showRemainingDays" class="model-entitlement-item is-remaining-days">
      <span>剩余天数</span>
      <strong class="model-entitlement-days-value">
        {{ remainingDaysValue }}
        <el-tooltip v-if="status.frozenTip" placement="top" :content="status.frozenTip">
          <button type="button" class="model-frozen-tip" aria-label="查看冻结权益说明">!</button>
        </el-tooltip>
      </strong>
    </div>

    <el-button
      v-if="status.authorizationVisible"
      class="model-entitlement-action"
      type="primary"
      round
      :size="compact ? 'default' : 'large'"
      title="点击后将在新标签页打开外部授权网站"
      @click="$emit('authorize')"
    >
      <span>{{ status.authorizationButtonText }}</span>
      <svg class="model-entitlement-external-icon" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
        <path d="M6.5 3.5H3.5V12.5H12.5V9.5" />
        <path d="M10 3H13V6" />
        <path d="M13 3L8.8 7.2" />
      </svg>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ElTooltip } from 'element-plus/es/components/tooltip/index.mjs';
import 'element-plus/es/components/tooltip/style/css';
import { computed } from 'vue';
import type { ModelEntitlementStatus } from '../../api/modelEntitlements';

const props = withDefaults(defineProps<{
  status: ModelEntitlementStatus | null;
  compact?: boolean;
  showRemainingDays?: boolean;
}>(), {
  showRemainingDays: true,
});

defineEmits<{
  authorize: [];
}>();

// 剩余天数只展示数字，永久权益保留后端文案避免误显示为0。
const remainingDaysValue = computed(() => {
  if (!props.status) {
    return '';
  }
  if (props.status.permanent) {
    return props.status.remainingDaysText;
  }
  return String(Math.max(0, props.status.remainingDays));
});
</script>
