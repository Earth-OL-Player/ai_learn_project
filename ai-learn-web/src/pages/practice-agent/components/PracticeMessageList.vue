<template>
  <section ref="panelRef" class="practice-message-panel" @scroll="emit('scroll')">
    <el-alert
      v-if="!isLoggedIn"
      class="guest-login-alert"
      title="游客可浏览 AI 智能刷题页面，开始练习、重答、发送答案等功能需要先注册登录。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-empty v-if="messages.length === 0" description="选择分类后点击开始，或直接输入想练习的题型" />
    <article v-for="item in messages" :key="item.id" class="practice-message" :class="[item.role, { 'is-streaming': item.streaming }]">
      <div class="message-bubble">
        <p v-if="item.streaming" class="message-text streaming-message-text">
          <span v-if="item.text">{{ item.text }}</span>
          <span v-else class="streaming-placeholder">{{ streamingPlaceholderText(item) }}</span>
          <span class="stream-cursor" aria-hidden="true"></span>
        </p>
        <div v-else-if="item.text" class="message-text message-markdown" v-html="renderSafeMessageText(item)"></div>
        <PracticeQuestionCard v-if="item.question" :question="item.question" />
        <PracticeGradingCard v-if="item.grading" :grading="item.grading" />
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { renderSafeMessageText } from '../practiceMessageFormat';
import type { ChatMessage } from '../types';
import PracticeGradingCard from './PracticeGradingCard.vue';
import PracticeQuestionCard from './PracticeQuestionCard.vue';

interface PracticeMessageListProps {
  isLoggedIn: boolean;
  messages: ChatMessage[];
  streamingPlaceholderText: (message: ChatMessage) => string;
}

const emit = defineEmits<{
  panelReady: [panel: HTMLElement | null];
  scroll: [];
}>();

defineProps<PracticeMessageListProps>();

const panelRef = ref<HTMLElement | null>(null);

onMounted(() => {
  emit('panelReady', panelRef.value);
});

onBeforeUnmount(() => {
  emit('panelReady', null);
});
</script>
