import type { PracticeMessageResult } from '../../api/practice';
import {
  SmoothStreamTypewriter,
  type SmoothStreamChunkEvent,
  type SmoothStreamRenderEvent,
} from '../../utils/smoothStreamTypewriter';
import type { ChatMessage } from './types';

const STREAM_INTERRUPTED_SUFFIX = '\n\n（本次回复已中断，已保留当前已生成内容。）';
const STREAM_INTERRUPTED_FALLBACK_TEXT = '本次回复中断，暂未收到可展示内容，请稍后重试。';

interface UseStreamTypewriterOptions {
  onCancelScheduledSnapshotSave: () => void;
  onFinishStreamingResult: (result: PracticeMessageResult, assistantMessage: ChatMessage) => void;
  onRefreshMessagesView: () => void;
  onSavePracticeSnapshot: () => void;
  onSchedulePracticeSnapshotSave: () => void;
  onScheduleScrollToBottom: () => void;
}

/**
 * 管理刷题页 SSE 平滑打字机和中断兜底。
 */
export function useStreamTypewriter(options: UseStreamTypewriterOptions) {
  let pendingStreamingResult: { result: PracticeMessageResult; assistantMessage: ChatMessage } | null = null;

  // 前端统一用平滑打字机重排 SSE 节奏，避免后端突发小片段直接抖动上屏。
  const streamTypewriter = new SmoothStreamTypewriter<ChatMessage>({
    onChunk: logFrontendStreamChunk,
    onRender: handleStreamRendered,
    onDrain: applyPendingStreamingResult,
  });

  /**
   * 追加流式文本片段。
   */
  function appendStreamingChunk(message: ChatMessage, chunk: string): void {
    streamTypewriter.appendChunk(message, chunk);
  }

  /**
   * 重置流式打字机状态。
   */
  function resetStreamTypewriter(message: ChatMessage): void {
    pendingStreamingResult = null;
    streamTypewriter.reset(message);
  }

  /**
   * 清理流式打字机状态。
   */
  function clearStreamTypewriter(): void {
    pendingStreamingResult = null;
    streamTypewriter.clear();
    options.onCancelScheduledSnapshotSave();
  }

  /**
   * 应用流式接口的最终响应。
   */
  function applyStreamingResult(result: PracticeMessageResult, assistantMessage: ChatMessage): void {
    // result 事件只把未展示尾部放进打字机，绝不直接覆盖气泡正文。
    streamTypewriter.queueFinalRemainder(assistantMessage, result.message);
    if (streamTypewriter.hasPendingOutput()) {
      pendingStreamingResult = { result, assistantMessage };
      return;
    }
    options.onFinishStreamingResult(result, assistantMessage);
  }

  /**
   * 等待打字机输出完成。
   */
  function waitForStreamIdle(): Promise<void> {
    return streamTypewriter.waitForIdle();
  }

  /**
   * 恢复快照中的流式消息状态。
   */
  function normalizeRestoredMessage(message: ChatMessage): ChatMessage {
    if (!message.streaming) {
      return message;
    }

    // 页面刷新后无法继续旧的浏览器流，保留已落盘文本并明确标记中断。
    return {
      ...message,
      text: buildInterruptedStreamingText(message.text),
      streaming: false,
      loadingText: undefined,
    };
  }

  /**
   * 保留异常中断前已经生成的流式消息。
   */
  function preserveInterruptedStreamingMessage(message: ChatMessage): void {
    streamTypewriter.flushPendingOutput();
    pendingStreamingResult = null;
    streamTypewriter.clear();
    message.streaming = false;
    message.loadingText = undefined;
    message.text = buildInterruptedStreamingText(message.text);
    options.onRefreshMessagesView();
    options.onSavePracticeSnapshot();
    options.onScheduleScrollToBottom();
  }

  /**
   * 在打字机输出完成后应用最终业务结果。
   */
  function applyPendingStreamingResult(): void {
    if (!pendingStreamingResult) {
      return;
    }

    const pendingResult = pendingStreamingResult;
    pendingStreamingResult = null;
    options.onFinishStreamingResult(pendingResult.result, pendingResult.assistantMessage);
  }

  /**
   * 处理打字机每一批实际上屏后的副作用。
   */
  function handleStreamRendered(event: SmoothStreamRenderEvent): void {
    logFrontendRenderedText(event);
    options.onRefreshMessagesView();
    options.onSchedulePracticeSnapshotSave();
    options.onScheduleScrollToBottom();
  }

  return {
    appendStreamingChunk,
    applyStreamingResult,
    clearStreamTypewriter,
    normalizeRestoredMessage,
    preserveInterruptedStreamingMessage,
    resetStreamTypewriter,
    waitForStreamIdle,
  };
}

/**
 * 生成流式中断后的可见提示文案。
 */
function buildInterruptedStreamingText(text: string): string {
  if (!text.trim()) {
    return STREAM_INTERRUPTED_FALLBACK_TEXT;
  }
  if (text.includes(STREAM_INTERRUPTED_SUFFIX.trim())) {
    return text;
  }
  return `${text}${STREAM_INTERRUPTED_SUFFIX}`;
}

/**
 * 记录前端打字机实际渲染片段。
 */
function logFrontendRenderedText(event: SmoothStreamRenderEvent): void {
  if (event.count !== 1 && event.count % 50 !== 0) {
    return;
  }

  // 只打印渲染长度和积压量，避免模型正文进入日志。
  console.info('前端平滑打字机已渲染片段', { count: event.count, chars: event.chars, queuedChars: event.queuedChars });
}

/**
 * 记录前端收到的流式片段。
 */
function logFrontendStreamChunk(event: SmoothStreamChunkEvent): void {
  if (event.count !== 1 && event.count % 50 !== 0) {
    return;
  }

  // 只打印片段长度和当前队列，避免用户答案或模型正文进入浏览器日志。
  console.info('前端收到 SSE 流式片段', { count: event.count, chars: event.chars, queuedChars: event.queuedChars });
}
