<template>
  <Teleport to="body">
    <div v-if="auth.isLoggedIn" class="site-assistant">
      <button v-if="!assistant.open" ref="launcher" class="assistant-launcher" aria-label="打开 AI 助手" :aria-expanded="assistant.open" @click="assistant.show()">
        <svg viewBox="0 0 24 24" width="26" height="26" aria-hidden="true"><path d="M12 3v3M8 3h8M5 8h14v11H5zM2 11v5m20-5v5M8 12h1m6 0h1M9 16h6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" /></svg>
        <span v-if="assistant.active" class="assistant-status-dot"></span>
      </button>

      <section v-if="assistant.open && !assistant.compact" ref="panel" class="assistant-panel" role="dialog" aria-label="AI 站内助手" tabindex="-1" @keydown.esc="close" @keydown.tab="containMobileFocus">
        <header class="assistant-panel-header">
          <div><strong>AI 站内助手</strong><small>{{ assistant.snapshot?.run.modelName || '与你的智能刷题使用相同模型' }}</small></div>
          <div class="assistant-header-actions">
            <button title="新会话" aria-label="开始新会话" @click="assistant.newSession()">＋</button>
            <button title="收起" aria-label="收起助手" @click="close">×</button>
          </div>
        </header>
        <div ref="messageArea" class="assistant-messages" aria-live="polite" aria-relevant="additions text">
          <div v-if="!assistant.messages.length" class="assistant-welcome">
            <span class="assistant-welcome-icon">✦</span><h3>需要我帮你做什么？</h3>
            <p>了解学习平台，打开页面，填写评论和建议。我会展示每一步操作。</p>
            <button v-for="prompt in prompts" :key="prompt" @click="send(prompt)">{{ prompt }} <span>↗</span></button>
          </div>
          <article v-for="(message, index) in assistant.messages" :key="index" :class="['assistant-message', `is-${message.role}`]">
            <span class="assistant-message-author">{{ message.role === 'user' ? '你' : 'AI 助手' }}</span>
            <div class="markdown-body" v-html="markdown.render(message.content)"></div>
            <div v-if="message.sources?.length" class="assistant-sources">
              <button v-for="source in message.sources" :key="source.id" @click="router.push(source.path)">{{ source.title }}</button>
            </div>
          </article>
          <div v-if="assistant.streaming" class="assistant-thinking">正在思考与处理…</div>
        </div>
        <div v-if="assistant.progress && assistant.active" class="assistant-inline-progress" role="status">{{ assistant.progress }}</div>
        <div v-if="assistant.error" class="assistant-error" role="alert">{{ assistant.error }}</div>
        <div v-if="assistant.waitingConfirmation" class="assistant-confirmation">
          <p>表单内容已填写。确认发布这份内容？</p>
          <blockquote>{{ assistant.operation?.payload.content }}</blockquote>
          <button class="assistant-primary" @click="assistant.confirm(true)">确认发布</button>
          <button @click="assistant.confirm(false)">保留草稿并取消</button>
        </div>
        <div v-if="assistant.conflictDraft !== null" class="assistant-conflict">
          <p>表单已有草稿，请选择：</p>
          <button @click="assistant.chooseDraft('replace')">保留原稿，填写新内容</button>
          <button @click="assistant.chooseDraft('append')">追加到原稿</button>
          <button @click="assistant.stop(true)">我来操作</button>
        </div>
        <div v-if="assistant.active" class="assistant-inline-controls">
          <button v-if="!assistant.busy && !assistant.waitingConfirmation" @click="assistant.resume()">继续任务</button>
          <button v-if="assistant.paused && assistant.formBusy" @click="assistant.chooseDraft('adopt')">采用表单正文</button>
          <button v-if="assistant.busy" @click="assistant.pause()">暂停</button>
          <button @click="assistant.stop()">停止</button>
          <button v-if="assistant.formBusy" @click="assistant.stop(true)">我来操作</button>
          <button @click="assistant.refresh()">查看状态</button>
        </div>
        <button v-if="assistant.savedDraft" class="assistant-restore-draft" @click="assistant.restoreDraft()">恢复保留的原草稿</button>
        <form class="assistant-input-area" @submit.prevent="send(input)">
          <textarea ref="inputElement" v-model="input" maxlength="4000" rows="2" placeholder="描述你的问题或操作…" aria-label="给助手的指令" :disabled="assistant.active || assistant.busy" @keydown.enter.exact.prevent="send(input)"></textarea>
          <button type="submit" class="assistant-send" :disabled="!input.trim() || assistant.busy || assistant.active" aria-label="发送">↑</button>
        </form>
        <footer class="assistant-panel-footer">Shift + Enter 换行 · 发布以实际执行结果为准</footer>
      </section>

      <section v-if="assistant.compact || (assistant.active && !assistant.open)" class="assistant-action-bar" aria-label="助手操作进度">
        <div class="assistant-action-bar-title"><span>✦ AI 助手</span><strong role="status">{{ assistant.progress || assistant.currentStep || '任务等待继续' }}</strong></div>
        <p v-if="assistant.error" class="assistant-error">{{ assistant.error }}</p>
        <div class="assistant-inline-controls">
          <button @click="assistant.show()">查看对话</button>
          <button v-if="assistant.waitingConfirmation" class="assistant-primary" @click="assistant.confirm()">确认发布</button>
          <button v-else-if="!assistant.busy" @click="assistant.resume()">继续</button>
          <button v-if="assistant.busy" @click="assistant.pause()">暂停</button>
          <button @click="assistant.stop()">停止</button>
          <button v-if="assistant.formBusy" @click="assistant.stop(true)">我来操作</button>
        </div>
      </section>
      <AssistantActionOverlay />
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';
import { useAssistantStore } from '../../stores/assistant';
import { createSafeMarkdownRenderer } from '../../utils/safeMarkdown';
import { resolveErrorMessage } from '../../utils/errorMessage';
import AssistantActionOverlay from './AssistantActionOverlay.vue';
import './assistant.scss';

const auth = useAuthStore();
const assistant = useAssistantStore();
const router = useRouter();
const route = useRoute();
assistant.configure(router);
const markdown = createSafeMarkdownRenderer({ breaks: true });
const input = ref('');
const launcher = ref<HTMLButtonElement | null>(null);
const panel = ref<HTMLElement | null>(null);
const inputElement = ref<HTMLTextAreaElement | null>(null);
const messageArea = ref<HTMLElement | null>(null);
const prompts = ['怎么开始 AI 智能刷题？', '打开评论区', '查看我的模型权益'];

async function send(content: string): Promise<void> {
  if (!content.trim()) return;
  try { input.value = ''; await assistant.send(content); }
  catch (cause) { assistant.error = resolveErrorMessage(cause); }
}
async function close(): Promise<void> {
  assistant.open = false;
  await nextTick();
  launcher.value?.focus();
}
function containMobileFocus(event: KeyboardEvent): void {
  if (window.innerWidth >= 768 || !panel.value) return;
  const controls = Array.from(panel.value.querySelectorAll<HTMLElement>('button:not(:disabled), textarea:not(:disabled), a[href]'))
    .filter(element => element.getClientRects().length > 0);
  const first = controls[0];
  const last = controls.at(-1);
  if (!first || !last) { event.preventDefault(); panel.value.focus(); return; }
  if (event.shiftKey && (document.activeElement === first || document.activeElement === panel.value)) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
}
watch(() => assistant.messages.map(message => message.content).join(''), async () => {
  await nextTick();
  messageArea.value?.scrollTo({ top: messageArea.value.scrollHeight, behavior: 'auto' });
});
watch(() => assistant.open && !assistant.compact, async visible => { if (visible) { await nextTick(); if (!inputElement.value?.disabled) inputElement.value?.focus(); else panel.value?.focus(); } });
watch(() => auth.user?.id, () => { input.value = ''; }, { flush: 'sync' });
watch(() => route.fullPath, () => { if (assistant.active && !assistant.navigating) assistant.userChangedForm(); });
</script>
