// 只要求目标对象提供可追加的文本字段，便于复用到不同消息模型。
export interface SmoothStreamTextTarget {
  text: string;
}

// 记录浏览器收到的 SSE 增量片段统计信息。
export interface SmoothStreamChunkEvent {
  count: number;
  chars: number;
  queuedChars: number;
}

// 记录每一批真正渲染到页面的文本统计信息。
export interface SmoothStreamRenderEvent {
  count: number;
  text: string;
  chars: number;
  queuedChars: number;
}

// 外层页面通过回调处理日志、滚动、快照保存和最终收尾。
export interface SmoothStreamTypewriterOptions {
  onChunk?: (event: SmoothStreamChunkEvent) => void;
  onRender?: (event: SmoothStreamRenderEvent) => void;
  onDrain?: () => void;
}

// 以 40 个中文字符估算一行，500ms 播完一行，形成一秒两行的节奏。
const STREAM_ESTIMATED_CHARS_PER_LINE = 40;
const STREAM_TARGET_LINE_DURATION_MILLIS = 500;
const STREAM_FRAME_INTERVAL_MILLIS = 32;
const STREAM_TARGET_CHARS_PER_MILLIS = STREAM_ESTIMATED_CHARS_PER_LINE / STREAM_TARGET_LINE_DURATION_MILLIS;

// 普通状态每批 2-4 字；积压严重时最多 8 字，仍避免整段闪现。
const STREAM_MIN_BATCH_CHARS = 2;
const STREAM_NORMAL_MAX_BATCH_CHARS = 4;
const STREAM_FAST_MAX_BATCH_CHARS = 6;
const STREAM_TURBO_MAX_BATCH_CHARS = 8;

// 按 4 行和 8 行积压量分级提速，兼顾自然感与长答案收敛速度。
const STREAM_FAST_QUEUE_CHARS = STREAM_ESTIMATED_CHARS_PER_LINE * 4;
const STREAM_TURBO_QUEUE_CHARS = STREAM_ESTIMATED_CHARS_PER_LINE * 8;
const STREAM_FAST_SPEED_MULTIPLIER = 1.25;
const STREAM_TURBO_SPEED_MULTIPLIER = 1.6;

// 限制后台标签页恢复后的累计预算，避免瞬间吐出过多文本。
const STREAM_MAX_ACCUMULATED_BUDGET_CHARS = STREAM_TURBO_MAX_BATCH_CHARS * 2;
const FINAL_REMAINDER_MIN_OVERLAP_CHARS = 4;
const FINAL_REMAINDER_MAX_OVERLAP_SCAN_CHARS = STREAM_ESTIMATED_CHARS_PER_LINE * 20;

/**
 * 平滑流式打字机：把后端 SSE 小碎片重新编排为稳定的“几字一跳”前端节奏。
 */
export class SmoothStreamTypewriter<T extends SmoothStreamTextTarget> {
  private target: T | null = null;

  private queueChars: string[] = [];

  private animationFrameId: number | undefined;

  private renderClockTimestamp = 0;

  private renderCharBudget = 0;

  private chunkCount = 0;

  private renderCount = 0;

  private idleResolvers: Array<() => void> = [];

  public constructor(private readonly options: SmoothStreamTypewriterOptions) {}

  /**
   * 重置并绑定本次正在输出的消息气泡。
   */
  public reset(target: T): void {
    this.clear();

    // 每次新回复都从干净的队列开始，防止上一轮残留污染新气泡。
    this.target = target;
  }

  /**
   * 追加后端实时增量片段。
   */
  public appendChunk(target: T, chunk: string): void {
    if (!chunk) {
      return;
    }

    // 后端可能很快连续推送，前端只入队，不直接整段上屏。
    this.ensureTarget(target);
    const appendedChars = this.enqueueText(chunk);
    this.chunkCount += 1;

    this.options.onChunk?.({
      count: this.chunkCount,
      chars: appendedChars,
      queuedChars: this.queueChars.length,
    });

    this.start();
  }

  /**
   * 将最终完整答案中尚未展示的尾部补入队列。
   */
  public queueFinalRemainder(target: T, finalMessage: string): void {
    if (!finalMessage) {
      return;
    }

    // result 事件只作为校准来源，避免最终答案瞬间覆盖打字机内容。
    this.ensureTarget(target);
    const remainder = this.resolveFinalRemainder(finalMessage);
    if (!remainder) {
      return;
    }

    this.enqueueText(remainder);
    this.start();
  }

  /**
   * 判断当前是否仍有文本等待视觉输出。
   */
  public hasPendingOutput(): boolean {
    return this.queueChars.length > 0 || this.animationFrameId !== undefined;
  }

  /**
   * 等待当前打字机完成视觉输出。
   */
  public waitForIdle(): Promise<void> {
    if (!this.hasPendingOutput()) {
      return Promise.resolve();
    }

    // 网络流结束后仍需等待前端队列播完，防止用户过早发送下一条打断动画。
    return new Promise((resolve) => {
      this.idleResolvers.push(resolve);
    });
  }

  /**
   * 清理动画帧和缓冲队列。
   */
  public clear(): void {
    this.cancelAnimationFrame();
    this.target = null;
    this.queueChars = [];

    // 所有节奏计数统一归零，下一次输出重新计算速度。
    this.renderClockTimestamp = 0;
    this.renderCharBudget = 0;
    this.chunkCount = 0;
    this.renderCount = 0;
    this.resolveIdleWaiters();
  }

  /**
   * 绑定目标消息，必要时安全切换到新的气泡。
   */
  private ensureTarget(target: T): void {
    if (this.target === target) {
      return;
    }

    // 正常情况下 loading 会防止并发请求，这里兜底处理异常切换。
    this.clear();
    this.target = target;
  }

  /**
   * 将文本拆成 Unicode 码点入队，避免中文和常见表情被截断。
   */
  private enqueueText(text: string): number {
    const chars = Array.from(text);
    chars.forEach((char) => {
      this.queueChars.push(char);
    });
    return chars.length;
  }

  /**
   * 启动下一帧渲染。
   */
  private start(): void {
    if (this.animationFrameId !== undefined || this.queueChars.length === 0) {
      return;
    }

    // 使用 requestAnimationFrame 对齐浏览器绘制，避免 setInterval 抢主线程。
    this.renderClockTimestamp = 0;
    this.animationFrameId = window.requestAnimationFrame(this.flushQueue);
  }

  /**
   * 按目标节奏消费缓冲队列。
   */
  private readonly flushQueue = (timestamp: number): void => {
    if (!this.target) {
      this.clear();
      return;
    }

    if (this.queueChars.length === 0) {
      this.stopAndDrain();
      return;
    }

    // 约 32ms 一跳，每跳 2-4 个字，约等于 500ms 展示一行。
    if (!this.shouldRenderFrame(timestamp)) {
      this.animationFrameId = window.requestAnimationFrame(this.flushQueue);
      return;
    }

    const text = this.takeNextText(this.resolveBatchSize());
    this.target.text += text;
    this.renderCount += 1;

    this.options.onRender?.({
      count: this.renderCount,
      text,
      chars: Array.from(text).length,
      queuedChars: this.queueChars.length,
    });

    this.animationFrameId = window.requestAnimationFrame(this.flushQueue);
  };

  /**
   * 判断当前帧是否达到可渲染的时间间隔。
   */
  private shouldRenderFrame(timestamp: number): boolean {
    if (this.renderClockTimestamp === 0) {
      this.renderClockTimestamp = timestamp - STREAM_FRAME_INTERVAL_MILLIS;
    }

    const elapsedMillis = timestamp - this.renderClockTimestamp;
    if (elapsedMillis < STREAM_FRAME_INTERVAL_MILLIS) {
      return false;
    }

    // 按“约 40 字一行、500ms 一行”积累字符预算。
    this.renderClockTimestamp = timestamp;
    this.renderCharBudget = Math.min(
      this.renderCharBudget + elapsedMillis * STREAM_TARGET_CHARS_PER_MILLIS * this.resolveSpeedMultiplier(),
      STREAM_MAX_ACCUMULATED_BUDGET_CHARS,
    );
    return this.renderCharBudget >= 1;
  }

  /**
   * 根据积压量计算本次最多渲染几个字。
   */
  private resolveBatchSize(): number {
    const maxBatchChars = this.resolveMaxBatchChars();
    const budgetChars = Math.max(STREAM_MIN_BATCH_CHARS, Math.floor(this.renderCharBudget));
    const batchSize = Math.min(this.queueChars.length, budgetChars, maxBatchChars);

    // 消费预算但保留小数余量，让后续批次自然出现 2/3/4 字节奏。
    this.renderCharBudget = Math.max(0, this.renderCharBudget - batchSize);
    return batchSize;
  }

  /**
   * 长答案积压时有限提速，但仍保持“几个字几个字”输出。
   */
  private resolveMaxBatchChars(): number {
    if (this.queueChars.length > STREAM_TURBO_QUEUE_CHARS) {
      return STREAM_TURBO_MAX_BATCH_CHARS;
    }
    if (this.queueChars.length > STREAM_FAST_QUEUE_CHARS) {
      return STREAM_FAST_MAX_BATCH_CHARS;
    }
    return STREAM_NORMAL_MAX_BATCH_CHARS;
  }

  /**
   * 长答案积压时提升预算增长速度，减少用户等待尾巴播完的时间。
   */
  private resolveSpeedMultiplier(): number {
    if (this.queueChars.length > STREAM_TURBO_QUEUE_CHARS) {
      return STREAM_TURBO_SPEED_MULTIPLIER;
    }
    if (this.queueChars.length > STREAM_FAST_QUEUE_CHARS) {
      return STREAM_FAST_SPEED_MULTIPLIER;
    }
    return 1;
  }

  /**
   * 从队列头部取出本次要展示的文本。
   */
  private takeNextText(maxChars: number): string {
    return this.queueChars.splice(0, maxChars).join('');
  }

  /**
   * 结束当前动画并通知外层流式结果可以收尾。
   */
  private stopAndDrain(): void {
    this.cancelAnimationFrame();

    // 队列真正播完后再应用评分卡片、题目卡片等最终业务结果。
    this.renderClockTimestamp = 0;
    this.renderCharBudget = 0;
    this.options.onDrain?.();
    this.resolveIdleWaiters();
  }

  /**
   * 取消浏览器动画帧。
   */
  private cancelAnimationFrame(): void {
    if (this.animationFrameId === undefined) {
      return;
    }

    window.cancelAnimationFrame(this.animationFrameId);
    this.animationFrameId = undefined;
  }

  /**
   * 计算最终答案相对当前已展示内容的剩余尾部。
   */
  private resolveFinalRemainder(finalMessage: string): string {
    if (!this.target) {
      return finalMessage;
    }

    const displayedOrQueuedText = this.target.text + this.queueChars.join('');
    if (!displayedOrQueuedText) {
      return finalMessage;
    }
    if (finalMessage.startsWith(displayedOrQueuedText)) {
      return finalMessage.slice(displayedOrQueuedText.length);
    }
    if (displayedOrQueuedText.startsWith(finalMessage)) {
      return '';
    }

    // 少数模型会在 result 中带轻微改写文本，优先找重叠尾部，避免重复整段答案。
    const includedIndex = finalMessage.indexOf(displayedOrQueuedText);
    if (includedIndex >= 0) {
      return finalMessage.slice(includedIndex + displayedOrQueuedText.length);
    }

    const overlapLength = this.findFinalMessageOverlap(finalMessage, displayedOrQueuedText);
    return overlapLength >= FINAL_REMAINDER_MIN_OVERLAP_CHARS ? finalMessage.slice(overlapLength) : '';
  }

  /**
   * 查找已展示文本尾部与最终答案头部的最长重叠。
   */
  private findFinalMessageOverlap(finalMessage: string, displayedOrQueuedText: string): number {
    const maxOverlapLength = Math.min(
      finalMessage.length,
      displayedOrQueuedText.length,
      FINAL_REMAINDER_MAX_OVERLAP_SCAN_CHARS,
    );

    for (let overlapLength = maxOverlapLength; overlapLength > 0; overlapLength -= 1) {
      if (displayedOrQueuedText.endsWith(finalMessage.slice(0, overlapLength))) {
        return overlapLength;
      }
    }
    return 0;
  }

  /**
   * 唤醒等待视觉输出结束的调用方。
   */
  private resolveIdleWaiters(): void {
    const resolvers = this.idleResolvers;
    this.idleResolvers = [];

    // 使用同步 resolve，确保 sendMessage 的 finally 在视觉输出后执行。
    resolvers.forEach((resolve) => resolve());
  }
}
