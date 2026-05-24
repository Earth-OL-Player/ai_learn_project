<template>
  <section
    ref="panelRef"
    class="practice-message-panel"
    tabindex="0"
    role="log"
    aria-label="刷题对话消息"
    aria-live="polite"
    aria-relevant="additions text"
    :aria-busy="hasStreamingMessage"
    @scroll="emit('scroll')"
  >
    <el-alert
      v-if="!isLoggedIn"
      class="guest-login-alert"
      title="游客可浏览 AI 智能刷题页面，开始练习、重答、发送答案等功能需要先注册登录。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-empty v-if="messages.length === 0" role="status" description="选择分类后点击开始，或直接输入想练习的题型" />
    <article
      v-for="item in messages"
      :key="item.id"
      class="practice-message"
      :class="[item.role, { 'is-streaming': item.streaming }]"
      :aria-label="messageAriaLabel(item)"
    >
      <div class="message-bubble">
        <p v-if="item.streaming" class="message-text streaming-message-text">
          <span v-if="item.text">{{ item.text }}</span>
          <span v-else class="streaming-placeholder">
            <span>{{ streamingPlaceholderText(item) }}</span>
            <span class="streaming-spinner" :style="thinkingSpinnerStyle" aria-hidden="true"></span>
          </span>
        </p>
        <div v-else-if="item.text" class="message-text message-markdown" v-html="renderSafeMessageText(item)"></div>
        <PracticeQuestionCard v-if="item.question" :question="item.question" />
        <PracticeGradingCard v-if="item.grading" :grading="item.grading" />
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { ElAlert } from 'element-plus/es/components/alert/index.mjs';
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs';
import 'element-plus/es/components/alert/style/css';
import 'element-plus/es/components/empty/style/css';
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
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

// 思考圆环使用 JS 驱动，避开全局 reduced-motion 对 CSS 动画的强制覆盖。
const FULL_ROTATION_DEGREES = 360;
const THINKING_INTERVAL_MS = 80;
const THINKING_STEP_DEGREES = 12;

const panelRef = ref<HTMLElement | null>(null);
const props = defineProps<PracticeMessageListProps>();
const hasStreamingMessage = computed(() => props.messages.some((message) => message.streaming));
const thinkingAngle = ref(0);
let thinkingTimerId: number | undefined;
let stopStreamingWatcher: (() => void) | undefined;

const thinkingSpinnerStyle = computed(() => ({
  transform: `rotate(${thinkingAngle.value}deg)`,
}));

/**
 * 生成消息的屏幕阅读器标签。
 */
function messageAriaLabel(message: ChatMessage): string {
  if (message.role === 'user') {
    return '我的消息';
  }
  return message.streaming ? 'AI 正在回复' : 'AI 回复';
}

/**
 * 开始驱动右侧思考圆环缓慢旋转。
 */
function startThinkingIndicator(): void {
  if (thinkingTimerId !== undefined) {
    return;
  }

  // 通过定时更新角度实现可见运动，不依赖 CSS animation。
  thinkingTimerId = window.setInterval(() => {
    thinkingAngle.value = (thinkingAngle.value + THINKING_STEP_DEGREES) % FULL_ROTATION_DEGREES;
  }, THINKING_INTERVAL_MS);
}

/**
 * 停止思考圆环的定时刷新。
 */
function stopThinkingIndicator(): void {
  if (thinkingTimerId === undefined) {
    return;
  }

  // 组件卸载或没有流式消息时及时释放定时器。
  window.clearInterval(thinkingTimerId);
  thinkingTimerId = undefined;
}

/**
 * 根据当前是否存在流式消息切换思考动画。
 */
function syncThinkingIndicator(isStreaming: boolean): void {
  if (isStreaming) {
    startThinkingIndicator();
    return;
  }
  stopThinkingIndicator();
}

onMounted(() => {
  emit('panelReady', panelRef.value);
  stopStreamingWatcher = watch(hasStreamingMessage, syncThinkingIndicator, { immediate: true });
});

onBeforeUnmount(() => {
  stopThinkingIndicator();
  stopStreamingWatcher?.();
  emit('panelReady', null);
});
</script>
