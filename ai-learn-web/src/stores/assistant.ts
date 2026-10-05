import { computed, nextTick, ref, shallowRef, watch } from 'vue';
import { defineStore } from 'pinia';
import type { Router } from 'vue-router';
import {
  acknowledgeAssistantAction, assistantControl, confirmAssistantOperation, createAssistantSession,
  fetchAssistantRun, fetchAssistantSession, streamAssistant, updateAssistantDraft,
} from '../api/assistant';
import { communityAdapter } from '../assistant/actionRegistry';
import { DraftConflict, executePageStep } from '../assistant/actionExecutor';
import type { AssistantActionRequest, AssistantMessage, AssistantOperation, AssistantSnapshot, CommunityKind } from '../types/assistant';
import { useAuthStore } from './auth';
import { resolveErrorMessage } from '../utils/errorMessage';

const TERMINAL = new Set(['COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED']);
const STEP_LABELS: Record<string, string> = { navigate: '打开页面', wait_ready: '等待表单', open_reply: '打开回复框', reveal: '定位表单', focus: '聚焦控件', select_option: '选择建议类型', fill: '填写内容', validate: '检查内容', submit: '提交内容' };

/** 账号隔离的助手会话。页面动作全部在当前标签页串行执行。 */
export const useAssistantStore = defineStore('assistant', () => {
  const auth = useAuthStore();
  const open = ref(false);
  const compact = ref(false);
  const sessionId = ref('');
  const messages = ref<AssistantMessage[]>([]);
  const snapshot = ref<AssistantSnapshot | null>(null);
  const streaming = ref(false);
  const executing = ref(false);
  const preparing = ref(false);
  const progress = ref('');
  const error = ref('');
  const conflictDraft = ref<string | null>(null);
  const savedDraft = ref<{ kind: CommunityKind; content: string; parentId?: string; type?: string } | null>(null);
  const selectedParentId = ref('');
  const target = shallowRef<HTMLElement | null>(null);
  const clickPulse = ref(0);
  const navigating = ref(false);
  const newId = () => globalThis.crypto?.randomUUID?.() ?? `${Date.now().toString(16)}-${Math.random().toString(16).slice(2)}`;
  const clientInstanceId = newId();
  let generation = 0;
  let taskVersion = 0;
  let abort: AbortController | null = null;
  let router: Router | null = null;
  let restorePromise: Promise<void> | null = null;
  const operation = computed(() => snapshot.value?.operations.find(op => op.status === 'PENDING') ?? null);
  const active = computed(() => Boolean(snapshot.value && !TERMINAL.has(snapshot.value.run.status)));
  const waitingConfirmation = computed(() => snapshot.value?.run.status === 'WAITING_CONFIRMATION');
  const paused = computed(() => snapshot.value?.run.status === 'PAUSED');
  const busy = computed(() => preparing.value || streaming.value || executing.value);
  const formBusy = computed(() => active.value && operation.value?.type !== 'navigate_page' && Boolean(operation.value));
  const currentStep = computed(() => operation.value ? STEP_LABELS[operation.value.steps[operation.value.nextStep] ?? ''] ?? progress.value : progress.value);

  function configure(value: Router): void { router = value; }
  function apply(value: AssistantSnapshot): void { snapshot.value = value; }
  function controlBody(): { clientInstanceId: string; executionEpoch: number } {
    return { clientInstanceId, executionEpoch: snapshot.value?.run.executionEpoch ?? 0 };
  }
  function actionBody(op: AssistantOperation): AssistantActionRequest {
    return { ...controlBody(), operationId: op.id, payloadVersion: op.payloadVersion, payloadHash: op.payloadHash, stepIndex: op.nextStep, status: 'completed' };
  }
  function storageKey(userId = auth.user?.id): string { return `ai-assistant-session:${userId}`; }
  function addAssistant(content: string): void { messages.value.push({ role: 'assistant', content }); }

  watch(() => auth.user?.id, (id, previous) => {
    generation++;
    taskVersion++;
    abort?.abort();
    open.value = false;
    compact.value = false;
    sessionId.value = '';
    messages.value = [];
    snapshot.value = null;
    target.value = null;
    error.value = '';
    conflictDraft.value = null;
    savedDraft.value = null;
    selectedParentId.value = '';
    streaming.value = false;
    executing.value = false;
    preparing.value = false;
    if (previous && previous !== id) sessionStorage.removeItem(storageKey(previous));
  }, { flush: 'sync' });

  async function restore(): Promise<void> {
    if (!auth.isLoggedIn || sessionId.value) return;
    if (restorePromise) return restorePromise;
    const currentGeneration = generation;
    const key = storageKey();
    restorePromise = (async () => {
      const stored = sessionStorage.getItem(key);
      if (!stored) return;
      try {
        const result = await fetchAssistantSession(stored);
        if (currentGeneration !== generation) return;
        sessionId.value = result.id;
        messages.value = result.messages.filter(message => (message.role === 'user' || message.role === 'assistant') && message.content);
        const last = [...result.runs].reverse().find(item => !TERMINAL.has(item.run.status));
        if (last) {
          apply(last);
          progress.value = '已恢复任务，请点击继续重新检查页面';
        }
      } catch { sessionStorage.removeItem(key); }
    })().finally(() => { restorePromise = null; });
    return restorePromise;
  }

  async function show(): Promise<void> {
    open.value = true;
    compact.value = false;
    await restore();
  }

  async function refresh(): Promise<void> {
    if (!snapshot.value) return;
    const currentGeneration = generation;
    const result = await fetchAssistantRun(snapshot.value.run.id);
    if (generation === currentGeneration) apply(result);
  }

  async function runStream(path: string, body: unknown): Promise<void> {
    const currentGeneration = generation;
    const version = ++taskVersion;
    abort?.abort();
    abort = new AbortController();
    const signal = abort.signal;
    streaming.value = true;
    error.value = '';
    let action: AssistantSnapshot | null = null;
    let draftIndex = -1;
    try {
      await streamAssistant(path, body, event => {
        if (currentGeneration !== generation || signal.aborted) return;
        const data = JSON.parse(event.data);
        if (event.event === 'meta' || event.event === 'done') apply(data as AssistantSnapshot);
        if (event.event === 'client_action') { apply(data as AssistantSnapshot); action = data as AssistantSnapshot; }
        if (event.event === 'text_delta') {
          if (draftIndex < 0) { draftIndex = messages.value.length; messages.value.push({ role: 'assistant', content: '' }); }
          messages.value[draftIndex].content += data.text ?? '';
        }
        if (event.event === 'model_result') {
          const generated = (data.messages ?? []) as AssistantMessage[];
          const final = generated.filter(message => message.role === 'assistant' && message.content).at(-1);
          if (draftIndex >= 0) {
            messages.value.splice(draftIndex, 1);
            draftIndex = -1;
          }
          generated.filter(message => message.role === 'assistant' && message.content).forEach(message => messages.value.push(message));
          if (final) progress.value = '';
        }
        if (event.event === 'tool_started') progress.value = '正在处理站内操作';
        if (event.event === 'error') error.value = data.message ?? '助手暂不可用';
      }, signal);
    } catch (cause) {
      if (!signal.aborted && currentGeneration === generation) {
        error.value = resolveErrorMessage(cause);
        try { await refresh(); } catch { /* 保留最后已知状态，用户可以稍后重查。 */ }
      }
    } finally {
      if (currentGeneration === generation && version === taskVersion) streaming.value = false;
    }
    if (action && currentGeneration === generation && !signal.aborted && version === taskVersion) await execute(action);
  }

  async function send(content: string): Promise<void> {
    if (!content.trim() || !router || busy.value || active.value) return;
    const currentGeneration = generation;
    preparing.value = true;
    try {
      await restore();
      if (currentGeneration !== generation || active.value) return;
      if (!sessionId.value) {
        const result = await createAssistantSession();
        if (currentGeneration !== generation) return;
        sessionId.value = result.id;
        sessionStorage.setItem(storageKey(), result.id);
      }
      messages.value.push({ role: 'user', content: content.trim() });
      const route = router.currentRoute.value;
      await runStream(`sessions/${sessionId.value}/messages/stream`, {
        content: content.trim(), clientRequestId: newId(), clientInstanceId,
        pageContext: { pageKey: route.path.slice(1), tab: route.query.tab, selectedParentId: selectedParentId.value },
      });
    } finally { if (currentGeneration === generation) preparing.value = false; }
  }

  async function execute(value: AssistantSnapshot): Promise<void> {
    if (!router || value.run.status !== 'WAITING_CLIENT') return;
    apply(value);
    if (executing.value) return;
    const currentGeneration = generation;
    const version = ++taskVersion;
    executing.value = true;
    compact.value = true;
    const assertActive = () => {
      if (generation !== currentGeneration || taskVersion !== version || !auth.isLoggedIn || snapshot.value?.run.status !== 'WAITING_CLIENT') throw new Error('页面动作已暂停');
      if (document.hidden) throw new Error('页面已切换到后台，请返回后继续');
    };
    try {
      while (operation.value && snapshot.value?.run.status === 'WAITING_CLIENT') {
        assertActive();
        const op = operation.value;
        const runId = snapshot.value.run.id;
        const body = actionBody(op);
        const step = op.steps[op.nextStep];
        progress.value = STEP_LABELS[step] ?? '正在操作页面';
        await executePageStep(router, op, step, { runId, request: body }, {
          assertActive,
          highlight: (element, label, click = false) => { if (currentGeneration !== generation) return; compact.value = true; target.value = element; progress.value = label; if (click) clickPulse.value++; },
          navigate: async task => { navigating.value = true; try { await task(); await nextTick(); } finally { navigating.value = false; } },
        });
        if (currentGeneration !== generation) return;
        if (step === 'submit') {
          await refresh();
          const completed = snapshot.value?.operations.find(item => item.id === op.id);
          addAssistant(completed?.result?.message ?? '提交结果已保存');
        } else {
          assertActive();
          const acknowledged = await acknowledgeAssistantAction(runId, body);
          if (currentGeneration !== generation || version !== taskVersion) return;
          apply(acknowledged);
        }
        if (waitingConfirmation.value) { progress.value = '草稿已填写，请确认是否发布'; break; }
      }
    } catch (cause) {
      if (currentGeneration !== generation) return;
      if (cause instanceof DraftConflict) conflictDraft.value = cause.draft;
      error.value = resolveErrorMessage(cause);
      if (version === taskVersion && snapshot.value?.run.status === 'WAITING_CLIENT') await pause();
    } finally {
      if (currentGeneration === generation) executing.value = false;
    }
    if (currentGeneration === generation && version === taskVersion && snapshot.value?.run.status === 'READY') {
      await runStream(`runs/${snapshot.value.run.id}/continue/stream`, controlBody());
    }
    if (!active.value) { compact.value = false; target.value = null; }
  }

  async function pause(): Promise<void> {
    const currentGeneration = generation;
    taskVersion++;
    abort?.abort();
    if (!snapshot.value || !active.value) return;
    progress.value = '操作已暂停，当前草稿保留';
    try { const result = await assistantControl(snapshot.value.run.id, 'pause', controlBody()); if (currentGeneration === generation) apply(result); }
    catch (cause) { if (currentGeneration === generation) error.value = resolveErrorMessage(cause); }
    if (currentGeneration === generation) streaming.value = false;
  }

  async function stop(takeOver = false): Promise<void> {
    taskVersion++;
    abort?.abort();
    const currentGeneration = generation;
    if (snapshot.value && active.value) {
      try { const result = await assistantControl(snapshot.value.run.id, 'cancel', controlBody()); if (currentGeneration === generation) apply(result); }
      catch (cause) { if (currentGeneration === generation) error.value = resolveErrorMessage(cause); }
    }
    if (currentGeneration !== generation) return;
    streaming.value = false;
    target.value = null;
    compact.value = false;
    progress.value = takeOver ? '已交给你操作，草稿保留在页面' : '已停止后续操作';
    addAssistant(progress.value);
    conflictDraft.value = null;
  }

  async function resume(): Promise<void> {
    if (!snapshot.value || busy.value) return;
    conflictDraft.value = null;
    await runStream(`runs/${snapshot.value.run.id}/resume/stream`, controlBody());
  }

  async function confirm(confirmed = true): Promise<void> {
    const currentGeneration = generation;
    const op = operation.value;
    if (!snapshot.value || !op || busy.value) return;
    try {
      const result = await confirmAssistantOperation(snapshot.value.run.id, { ...actionBody(op), confirmed });
      if (currentGeneration !== generation) return;
      apply(result);
      if (confirmed) await runStream(`runs/${snapshot.value.run.id}/continue/stream`, controlBody());
      else { compact.value = false; target.value = null; }
    } catch (cause) { if (currentGeneration === generation) error.value = resolveErrorMessage(cause); }
  }

  async function chooseDraft(mode: 'replace' | 'append' | 'adopt'): Promise<void> {
    const currentGeneration = generation;
    const op = operation.value;
    const adapter = communityAdapter.value;
    if (!op?.payload.kind || !snapshot.value || !adapter) return;
    const draft = adapter.readDraft(op.payload.kind);
    const content = mode === 'adopt' ? draft.content : mode === 'append' ? `${draft.content}\n${op.payload.content ?? ''}` : op.payload.content ?? '';
    try {
      savedDraft.value = { kind: op.payload.kind, ...draft };
      const result = await updateAssistantDraft(snapshot.value.run.id, { ...controlBody(), operationId: op.id, payloadVersion: op.payloadVersion, content });
      if (currentGeneration !== generation) return;
      apply(result);
      // 原内容在本地恢复槽中保存；用户明确选择后才清空，以免重复触发冲突。
      adapter.setContent(op.payload.kind, '');
      conflictDraft.value = null;
      await resume();
    } catch (cause) { if (currentGeneration === generation) error.value = resolveErrorMessage(cause); }
  }

  async function restoreDraft(): Promise<void> {
    const currentGeneration = generation;
    const saved = savedDraft.value;
    const adapter = communityAdapter.value;
    if (!saved || !adapter) return;
    if (active.value) await stop(true);
    if (currentGeneration !== generation) return;
    if (saved.kind === 'reply' && saved.parentId) await adapter.openReply(saved.parentId);
    if (currentGeneration !== generation) return;
    adapter.setContent(saved.kind, saved.content);
    if (saved.type) adapter.selectType(saved.type);
    savedDraft.value = null;
  }

  async function newSession(): Promise<void> {
    const currentGeneration = generation;
    if (active.value) await stop();
    if (currentGeneration !== generation || busy.value) return;
    sessionStorage.removeItem(storageKey());
    sessionId.value = '';
    messages.value = [];
    snapshot.value = null;
    error.value = '';
    progress.value = '';
  }

  function userChangedForm(): void {
    if (active.value && !navigating.value) void pause();
  }

  document.addEventListener('visibilitychange', () => {
    if (document.hidden && active.value && (executing.value || waitingConfirmation.value)) void pause();
  });

  return { open, compact, messages, snapshot, streaming, executing, progress, error, conflictDraft, savedDraft,
    target, clickPulse, navigating, selectedParentId, operation, active, waitingConfirmation, paused, busy, formBusy,
    currentStep, configure, show, send, restore, refresh, pause, stop, resume, confirm, chooseDraft, restoreDraft, newSession, userChangedForm };
});
