<template>
  <div class="practice-input-bar">
    <el-input
      v-model="inputValue"
      type="textarea"
      :rows="3"
      resize="none"
      :placeholder="placeholder"
      @focus="emit('focusInput')"
      @keydown.enter.exact.prevent="emit('send')"
    />
    <el-button type="primary" round :loading="loading" @click="emit('send')">发送</el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface PracticeInputBarProps {
  loading: boolean;
  modelValue: string;
  placeholder: string;
}

const props = defineProps<PracticeInputBarProps>();

const emit = defineEmits<{
  focusInput: [];
  send: [];
  'update:modelValue': [value: string];
}>();

// 输入栏只负责收集文本，发送行为交给页面业务组合函数处理。
const inputValue = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
});
</script>
