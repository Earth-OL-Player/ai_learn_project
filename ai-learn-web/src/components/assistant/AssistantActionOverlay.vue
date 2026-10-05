<template>
  <div class="assistant-action-overlay" aria-hidden="true">
    <div v-if="rect" class="assistant-target-highlight" :style="highlightStyle"></div>
    <div v-if="rect" class="assistant-visual-cursor" :style="cursorStyle">
      <svg width="22" height="26" viewBox="0 0 22 26"><path d="M2 2 L2 21 L7 16 L12 24 L16 22 L11 14 L19 14 Z" fill="currentColor" stroke="white" stroke-width="2" /></svg>
      <span>AI</span>
      <i :key="assistant.clickPulse" v-if="assistant.clickPulse" class="assistant-click-wave"></i>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useAssistantStore } from '../../stores/assistant';

const assistant = useAssistantStore();
const rect = ref<{ left: number; top: number; width: number; height: number } | null>(null);
let observer: ResizeObserver | null = null;
let frame = 0;
function update(): void {
  cancelAnimationFrame(frame);
  frame = requestAnimationFrame(() => {
    const element = assistant.target;
    if (!element?.isConnected || !element.getClientRects().length) { rect.value = null; return; }
    const bounds = element.getBoundingClientRect();
    rect.value = { left: bounds.left, top: bounds.top, width: bounds.width, height: bounds.height };
  });
}
watch(() => assistant.target, element => {
  observer?.disconnect();
  if (element) { observer = new ResizeObserver(update); observer.observe(element); }
  update();
});
const highlightStyle = computed(() => rect.value ? { left: `${rect.value.left - 4}px`, top: `${rect.value.top - 4}px`, width: `${rect.value.width + 8}px`, height: `${rect.value.height + 8}px` } : {});
const cursorStyle = computed(() => rect.value ? { left: `${Math.min(innerWidth - 60, rect.value.left + rect.value.width * 0.5)}px`, top: `${Math.max(8, Math.min(innerHeight - 80, rect.value.top + rect.value.height * 0.5))}px` } : {});
onMounted(() => { window.addEventListener('scroll', update, true); window.addEventListener('resize', update); window.visualViewport?.addEventListener('resize', update); });
onBeforeUnmount(() => { observer?.disconnect(); cancelAnimationFrame(frame); window.removeEventListener('scroll', update, true); window.removeEventListener('resize', update); window.visualViewport?.removeEventListener('resize', update); });
</script>
