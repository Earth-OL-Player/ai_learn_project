import { shallowRef } from 'vue';
import type { AssistantPayload, AssistantPublishResult, AssistantSubmitContext, CommunityKind } from '../types/assistant';

/** 本站代码注册操作能力，模型不能提供 CSS/JS 或任意控件。 */
export interface CommunityAdapter {
  element: HTMLElement;
  isReady: () => boolean;
  readDraft: (kind: CommunityKind) => { content: string; parentId?: string; type?: string };
  resolve: (payload: AssistantPayload, target: 'content' | 'submit' | 'type' | 'reply') => HTMLElement | null;
  openReply: (parentId: string) => Promise<void>;
  setContent: (kind: CommunityKind, content: string) => void;
  selectType: (type: string) => void;
  validate: (payload: AssistantPayload) => void;
  submit: (payload: AssistantPayload, context: AssistantSubmitContext) => Promise<AssistantPublishResult>;
  revealResult: (kind: CommunityKind, id: string) => Promise<HTMLElement | null>;
}

export const communityAdapter = shallowRef<CommunityAdapter | null>(null);

export function registerCommunityAdapter(adapter: CommunityAdapter): () => void {
  communityAdapter.value = adapter;
  return () => { if (communityAdapter.value === adapter) communityAdapter.value = null; };
}
