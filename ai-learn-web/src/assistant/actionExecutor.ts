import { nextTick } from 'vue';
import type { Router } from 'vue-router';
import { communityAdapter, type CommunityAdapter } from './actionRegistry';
import type { AssistantOperation, AssistantPayload, AssistantSubmitContext } from '../types/assistant';

const PAGES = new Set(['home', 'learning-roadmap', 'practice-agent', 'interview-questions', 'suggestions-comments', 'profile']);

export class DraftConflict extends Error {
  constructor(public readonly draft: string) { super('表单已有未提交内容，请选择如何处理'); }
}

export interface ActionRuntime {
  assertActive: () => void;
  highlight: (element: HTMLElement | null, label: string, click?: boolean) => void;
  navigate: (task: () => Promise<unknown>) => Promise<void>;
}

async function waitForAdapter(runtime: ActionRuntime): Promise<CommunityAdapter> {
  const deadline = Date.now() + 10000;
  while (Date.now() < deadline) {
    runtime.assertActive();
    if (communityAdapter.value?.element.isConnected && communityAdapter.value.isReady()) return communityAdapter.value;
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  throw new Error('页面表单尚未加载，请重试或接管操作');
}

function target(adapter: CommunityAdapter, payload: AssistantPayload, key: 'content' | 'submit' | 'type' | 'reply'): HTMLElement {
  const element = adapter.resolve(payload, key);
  if (!element?.isConnected || element.getClientRects().length === 0) throw new Error('目标控件不可见，已停止后续操作');
  if (element instanceof HTMLButtonElement && element.disabled) throw new Error('目标按钮当前不可用');
  return element;
}

async function delay(runtime: ActionRuntime, ms = 180): Promise<void> {
  await new Promise(resolve => setTimeout(resolve, window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : ms));
  runtime.assertActive();
}

export async function executePageStep(router: Router, operation: AssistantOperation, step: string, context: AssistantSubmitContext, runtime: ActionRuntime): Promise<void> {
  runtime.assertActive();
  const payload = operation.payload;
  const kind = payload.kind ?? 'comment';
  if (step === 'navigate') {
    const page = operation.type === 'navigate_page' ? payload.pageKey ?? 'home' : 'suggestions-comments';
    if (!PAGES.has(page)) throw new Error('该页面未接入助手');
    const tab = operation.type === 'navigate_page' ? payload.tab : kind === 'suggestion' ? 'suggestions' : 'comments';
    runtime.highlight(null, '正在打开页面');
    await runtime.navigate(() => router.push({ path: `/${page}`, query: page === 'suggestions-comments' ? { tab: tab === 'comments' ? 'comments' : 'suggestions' } : {} }));
    await nextTick();
    return;
  }
  const adapter = await waitForAdapter(runtime);
  if (router.currentRoute.value.path !== '/suggestions-comments') throw new Error('页面已改变，请重新定位操作目标');
  if (step === 'wait_ready') return;
  if (step === 'open_reply') {
    const draft = adapter.readDraft('reply');
    if (draft.content && draft.parentId !== payload.parentId) throw new DraftConflict(draft.content);
    await adapter.openReply(payload.parentId ?? '');
    runtime.assertActive();
    await nextTick();
    return;
  }
  if (step === 'select_option') {
    adapter.selectType(payload.type ?? 'FEATURE');
    await nextTick();
    runtime.highlight(target(adapter, payload, 'type'), '正在选择建议类型', true);
    await delay(runtime);
    return;
  }
  const element = target(adapter, payload, step === 'submit' ? 'submit' : 'content');
  if (step === 'reveal') {
    element.scrollIntoView({ behavior: 'auto', block: 'center' });
    runtime.highlight(element, '已定位表单');
    await delay(runtime);
  } else if (step === 'focus') {
    // 手机自动操作不弹出软键盘，以免遮住目标；接管后用户自行聚焦。
    if (window.innerWidth >= 768) element.focus({ preventScroll: true });
    runtime.highlight(element, '准备填写内容');
  } else if (step === 'fill') {
    const draft = adapter.readDraft(kind);
    const content = payload.content ?? '';
    if (draft.content && draft.content !== content) throw new DraftConflict(draft.content);
    runtime.highlight(element, '正在填写内容');
    const chunks = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 1 : 4;
    for (let i = 1; i <= chunks; i++) {
      runtime.assertActive();
      adapter.setContent(kind, content.slice(0, Math.ceil(content.length * i / chunks)));
      await delay(runtime, 140);
    }
    if (adapter.readDraft(kind).content !== content) throw new Error('表单内容发生变化，请重新确认');
  } else if (step === 'validate') {
    adapter.validate(payload);
    runtime.highlight(element, '内容已填写并通过校验');
  } else if (step === 'submit') {
    adapter.validate(payload);
    runtime.assertActive();
    element.scrollIntoView({ behavior: 'auto', block: 'center' });
    await nextTick();
    runtime.highlight(element, '正在提交', true);
    const result = await adapter.submit(payload, context);
    // 写入已完成时，即使用户此时停止，也应呈现真实结果。
    if (!result.success || !result.record) throw new Error(result.message || '提交失败');
    const createdElement = await adapter.revealResult(kind, result.record.id);
    if (createdElement) {
      createdElement.scrollIntoView({ behavior: 'auto', block: 'center' });
      runtime.highlight(createdElement, '发布成功');
    } else runtime.highlight(null, '已发布，可在社区列表查看');
  } else throw new Error('页面动作未注册');
}
