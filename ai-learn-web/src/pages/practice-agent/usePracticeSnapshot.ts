import { nextTick, type Ref } from 'vue';
import type { PracticePhase, PracticeQuestion } from '../../api/practice';
import type { ChatMessage, PracticeChatSnapshot } from './types';

const PRACTICE_CHAT_STORAGE_PREFIX = 'ai_learn_practice_chat_';
const STREAM_SNAPSHOT_SAVE_INTERVAL_MILLIS = 800;
const MESSAGE_PANEL_BOTTOM_THRESHOLD_PX = 72;

interface UsePracticeSnapshotOptions {
  phase: Ref<PracticePhase>;
  currentQuestion: Ref<PracticeQuestion | null>;
  messages: Ref<ChatMessage[]>;
  messagePanelRef: Ref<HTMLElement | null>;
  resolveUserId: () => string | number | undefined;
}

/**
 * 管理刷题页本地快照和消息面板滚动记忆。
 */
export function usePracticeSnapshot(options: UsePracticeSnapshotOptions) {
  const isMessagePanelPinnedToBottom = { value: true };
  let scrollAnimationFrame: number | undefined;
  let snapshotSaveTimer: number | undefined;
  let rememberedMessagePanelScrollTop = 0;
  let isRestoringMessagePanelScroll = false;

  /**
   * 读取当前用户的本地聊天快照。
   */
  function readPracticeSnapshot(): PracticeChatSnapshot | null {
    try {
      const rawValue = localStorage.getItem(practiceChatStorageKey());
      if (!rawValue) {
        return null;
      }
      return JSON.parse(rawValue) as PracticeChatSnapshot;
    } catch {
      clearPracticeSnapshot();
      return null;
    }
  }

  /**
   * 保存当前题聊天快照。
   */
  function savePracticeSnapshot(): void {
    if (!options.currentQuestion.value) {
      return;
    }
    const snapshot: PracticeChatSnapshot = {
      phase: options.phase.value,
      questionCode: options.currentQuestion.value.code,
      messages: options.messages.value,
      scrollTop: resolveMessagePanelScrollTop(),
      pinnedToBottom: isMessagePanelPinnedToBottom.value,
      updatedAt: Date.now(),
    };

    // localStorage 仅保存当前用户当前题轻量对话状态，不保存真实密钥或敏感配置。
    localStorage.setItem(practiceChatStorageKey(), JSON.stringify(snapshot));
  }

  /**
   * 延迟保存聊天快照，减少流式输出期间同步 localStorage 阻塞主线程。
   */
  function schedulePracticeSnapshotSave(): void {
    if (snapshotSaveTimer !== undefined) {
      return;
    }
    snapshotSaveTimer = window.setTimeout(flushScheduledSnapshotSave, STREAM_SNAPSHOT_SAVE_INTERVAL_MILLIS);
  }

  /**
   * 立即落盘已延迟的聊天快照。
   */
  function flushScheduledSnapshotSave(): void {
    cancelScheduledSnapshotSave();
    savePracticeSnapshot();
  }

  /**
   * 取消待执行的快照保存任务。
   */
  function cancelScheduledSnapshotSave(): void {
    if (snapshotSaveTimer === undefined) {
      return;
    }
    window.clearTimeout(snapshotSaveTimer);
    snapshotSaveTimer = undefined;
  }

  /**
   * 清理当前用户的本地聊天快照。
   */
  function clearPracticeSnapshot(): void {
    localStorage.removeItem(practiceChatStorageKey());
  }

  /**
   * 构造当前用户聊天快照键。
   */
  function practiceChatStorageKey(): string {
    return `${PRACTICE_CHAT_STORAGE_PREFIX}${options.resolveUserId() ?? 'anonymous'}`;
  }

  /**
   * 滚动到消息底部。
   */
  function scrollToBottom(force = true): void {
    nextTick(() => {
      scrollMessagePanelToBottom(force);
    });
  }

  /**
   * 合并滚动请求，避免流式逐字渲染时反复触发布局计算。
   */
  function scheduleScrollToBottom(force = false): void {
    if (!force && !shouldAutoScrollMessagePanel()) {
      return;
    }
    if (scrollAnimationFrame !== undefined) {
      return;
    }
    scrollAnimationFrame = window.requestAnimationFrame(() => {
      scrollAnimationFrame = undefined;
      scrollMessagePanelToBottom(force);
    });
  }

  /**
   * 处理用户主动滚动消息面板。
   */
  function handleMessagePanelScroll(): void {
    if (isRestoringMessagePanelScroll) {
      return;
    }
    const panel = options.messagePanelRef.value;
    if (!panel) {
      return;
    }

    // 用户离开底部后暂停自动跟随，避免流式输出抢夺滚轮控制权。
    rememberedMessagePanelScrollTop = panel.scrollTop;
    isMessagePanelPinnedToBottom.value = isMessagePanelNearBottom(panel);
    schedulePracticeSnapshotSave();
  }

  /**
   * 恢复消息面板滚动位置。
   */
  function restoreMessagePanelScroll(scrollTop = rememberedMessagePanelScrollTop, pinnedToBottom = isMessagePanelPinnedToBottom.value): void {
    rememberedMessagePanelScrollTop = scrollTop;
    isMessagePanelPinnedToBottom.value = pinnedToBottom;
    isRestoringMessagePanelScroll = true;
    nextTick(() => {
      window.requestAnimationFrame(() => restorePanelScrollAfterRender(scrollTop, pinnedToBottom));
    });
  }

  /**
   * 保存离开页面前的滚动状态。
   */
  function saveMessagePanelStateBeforeLeave(): void {
    rememberMessagePanelScrollState();
    flushScheduledSnapshotSave();
    savePracticeSnapshot();
  }

  /**
   * 释放快照和滚动相关的浏览器任务。
   */
  function disposePracticeSnapshot(): void {
    cancelScheduledSnapshotSave();
    if (scrollAnimationFrame !== undefined) {
      window.cancelAnimationFrame(scrollAnimationFrame);
      scrollAnimationFrame = undefined;
    }
  }

  /**
   * 渲染后恢复消息面板滚动位置。
   */
  function restorePanelScrollAfterRender(scrollTop: number, pinnedToBottom: boolean): void {
    const panel = options.messagePanelRef.value;
    if (!panel) {
      isRestoringMessagePanelScroll = false;
      return;
    }
    if (pinnedToBottom) {
      scrollMessagePanelToBottom(true);
      isRestoringMessagePanelScroll = false;
      return;
    }

    // 历史位置超过当前内容高度时做兜底裁剪，避免恢复后出现空白区域。
    panel.scrollTop = Math.min(scrollTop, Math.max(panel.scrollHeight - panel.clientHeight, 0));
    rememberedMessagePanelScrollTop = panel.scrollTop;
    isMessagePanelPinnedToBottom.value = isMessagePanelNearBottom(panel);
    isRestoringMessagePanelScroll = false;
  }

  /**
   * 将消息面板滚动到最新消息位置。
   */
  function scrollMessagePanelToBottom(force = false): void {
    const panel = options.messagePanelRef.value;
    if (!panel || (!force && !shouldAutoScrollMessagePanel())) {
      return;
    }
    panel.scrollTop = panel.scrollHeight;
    rememberedMessagePanelScrollTop = panel.scrollTop;
    isMessagePanelPinnedToBottom.value = true;
  }

  /**
   * 判断消息面板是否应该继续自动贴底。
   */
  function shouldAutoScrollMessagePanel(): boolean {
    const panel = options.messagePanelRef.value;
    return !panel || isMessagePanelPinnedToBottom.value || isMessagePanelNearBottom(panel);
  }

  /**
   * 判断消息面板当前是否接近底部。
   */
  function isMessagePanelNearBottom(panel: HTMLElement): boolean {
    return panel.scrollHeight - panel.scrollTop - panel.clientHeight <= MESSAGE_PANEL_BOTTOM_THRESHOLD_PX;
  }

  /**
   * 读取当前消息面板滚动位置。
   */
  function resolveMessagePanelScrollTop(): number {
    return rememberedMessagePanelScrollTop;
  }

  /**
   * 记住当前消息面板滚动状态。
   */
  function rememberMessagePanelScrollState(): void {
    const panel = options.messagePanelRef.value;
    if (!panel || isRestoringMessagePanelScroll) {
      return;
    }

    // 路由离开前主动读取真实滚动位置，避免组件停用后被默认 0 覆盖。
    rememberedMessagePanelScrollTop = panel.scrollTop;
    isMessagePanelPinnedToBottom.value = isMessagePanelNearBottom(panel);
  }

  return {
    cancelScheduledSnapshotSave,
    clearPracticeSnapshot,
    disposePracticeSnapshot,
    flushScheduledSnapshotSave,
    handleMessagePanelScroll,
    readPracticeSnapshot,
    restoreMessagePanelScroll,
    saveMessagePanelStateBeforeLeave,
    savePracticeSnapshot,
    schedulePracticeSnapshotSave,
    scheduleScrollToBottom,
    scrollToBottom,
  };
}
