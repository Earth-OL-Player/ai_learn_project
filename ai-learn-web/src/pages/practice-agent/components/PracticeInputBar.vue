<template>
  <div class="practice-input-bar" role="form" aria-label="提交答案">
    <label class="sr-only" for="practice-answer-input">答案输入框</label>
    <el-input
      id="practice-answer-input"
      v-model="inputValue"
      type="textarea"
      :autosize="inputTextareaAutosize"
      resize="none"
      :placeholder="placeholder"
      :disabled="loading"
      aria-label="答案输入框"
      aria-describedby="practice-answer-help"
      @focus="emit('focusInput')"
      @keydown.enter.exact.prevent="emit('send')"
    />
    <span id="practice-answer-help" class="sr-only">按 Enter 提交答案，按 Shift 加 Enter 换行。</span>
    <el-button
      type="primary"
      round
      :disabled="!inputValue.trim()"
      :loading="loading"
      aria-label="发送答案"
      @click="emit('send')"
    >
      发送
    </el-button>
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

const INPUT_TEXTAREA_MIN_ROWS = 3;
const INPUT_TEXTAREA_MAX_ROWS = 10;

// 文本框先随内容向上增高，超过十行后固定高度并交给内部滚动。
const inputTextareaAutosize = {
  minRows: INPUT_TEXTAREA_MIN_ROWS,
  maxRows: INPUT_TEXTAREA_MAX_ROWS,
};

// 输入栏只负责收集文本，发送行为交给页面业务组合函数处理。
const inputValue = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
});
</script>
