import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, triggerRef, watch } from 'vue';
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router';
import {
  fetchNextPracticeQuestion,
  fetchPracticeCategories,
  fetchPracticeState,
  retryPracticeQuestion,
  sendPracticeMessageStream,
  type PracticeGrading,
  type PracticeChatHistoryMessage,
  type PracticeMessageResult,
  type PracticePhase,
  type PracticeQuestion,
  type PracticeState,
} from '../../api/practice';
import { fetchModelEntitlementStatus, type ModelEntitlementStatus } from '../../api/modelEntitlements';
import { useAuthStore } from '../../stores/auth';
import type { BadgeInfo, GrowthInfo } from '../../types/growth';
import { resolveErrorMessage } from '../../utils/errorMessage';
import { openModelAuthorization } from '../../utils/modelAuthorization';
import { resolveUserDisplayName } from '../../utils/userDisplay';
import type { ChatMessage } from './types';
import { usePracticeSnapshot } from './usePracticeSnapshot';
import { useStreamTypewriter } from './useStreamTypewriter';

const GUEST_LOGIN_MESSAGE = '注册登录后即可使用该功能';

/**
 * 编排 AI 刷题页业务状态、接口调用和生命周期。
 */
export function usePracticeChat() {
  const loading = ref(false);
  const inputText = ref('');
  const phase = ref<PracticePhase>('QUESTIONING');
  const categories = ref<string[]>([]);
  const selectedCategories = ref<string[]>([]);
  const currentQuestion = ref<PracticeQuestion | null>(null);
  const growth = ref<GrowthInfo | null>(null);
  const modelEntitlementStatus = ref<ModelEntitlementStatus | null>(null);
  const messages = ref<ChatMessage[]>([]);
  const messagePanelRef = ref<HTMLElement | null>(null);
  const badgeDialogVisible = ref(false);
  const badgeDialogBadges = ref<BadgeInfo[]>([]);
  const authStore = useAuthStore();
  const route = useRoute();
  const router = useRouter();
  let messageId = 1;

  const snapshot = usePracticeSnapshot({
    phase,
    currentQuestion,
    messages,
    messagePanelRef,
    resolveUserId: () => authStore.user?.id,
  });

  const stream = useStreamTypewriter({
    onCancelScheduledSnapshotSave: snapshot.cancelScheduledSnapshotSave,
    onFinishStreamingResult: finishStreamingResult,
    onRefreshMessagesView: refreshMessagesView,
    onSavePracticeSnapshot: snapshot.savePracticeSnapshot,
    onSchedulePracticeSnapshotSave: snapshot.schedulePracticeSnapshotSave,
    onScheduleScrollToBottom: snapshot.scheduleScrollToBottom,
  });

  // 左侧角色卡昵称保持与个人中心、顶部用户菜单一致。
  const displayName = computed(() => resolveUserDisplayName(authStore.user));

  // 按阶段展示主按钮文案，首次进入是开始，后续是下一题。
  const nextButtonText = computed(() => (phase.value === 'QUESTIONING' ? '开始' : '下一题'));

  // 输入框提示根据当前阶段变化，引导用户输入最合适的内容。
  const inputPlaceholder = computed(() => {
    if (!authStore.isLoggedIn) {
      return '注册登录后即可开始练习、提交答案和获得成长经验';
    }
    if (phase.value === 'ANSWERING') {
      return '请输入你的答案';
    }
    if (phase.value === 'DISCUSSING') {
      return '请输入你的疑惑';
    }
    return '请告诉AI你想练的题';
  });

  // 未登录状态下仍允许点击重答按钮，以便明确提示注册登录要求。
  const retryButtonDisabled = computed(() => authStore.isLoggedIn && !currentQuestion.value);

  // 勋章弹框副标题按数量动态展示，多个勋章时突出批量点亮的成就感。
  const badgeAwardSubtitle = computed(() => {
    const badgeCount = badgeDialogBadges.value.length;
    if (badgeCount > 1) {
      return `一次点亮 ${badgeCount} 枚勋章，今天的修炼收获满满。`;
    }
    return '新的学习成就已点亮，继续保持。';
  });

  /**
   * 初始化刷题页面。
   */
  async function initializePage(): Promise<void> {
    loading.value = true;
    try {
      if (!authStore.isLoggedIn) {
        await initializeGuestPage();
        return;
      }
      const [state, entitlementStatus] = await Promise.all([
        fetchPracticeState(),
        fetchModelEntitlementStatus(),
      ]);
      phase.value = state.phase;
      categories.value = state.questionTypes;
      currentQuestion.value = state.currentQuestion;
      growth.value = state.growth;
      modelEntitlementStatus.value = entitlementStatus;
      syncAuthGrowth(state.growth);
      if (state.currentQuestion) {
        restorePracticeSnapshot(state);
      } else {
        messages.value = [];
        snapshot.clearPracticeSnapshot();
      }
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, '刷题状态加载失败'));
    } finally {
      loading.value = false;
    }
  }

  /**
   * 初始化游客可浏览的刷题页面外壳。
   */
  async function initializeGuestPage(): Promise<void> {
    const [nextCategories, entitlementStatus] = await Promise.all([
      fetchPracticeCategories(),
      fetchModelEntitlementStatus(),
    ]);
    categories.value = nextCategories;
    modelEntitlementStatus.value = entitlementStatus;
    phase.value = 'QUESTIONING';
    currentQuestion.value = null;
    growth.value = null;
    messages.value = [];
    snapshot.clearPracticeSnapshot();
  }

  /**
   * 抽取下一题。
   */
  async function handleNextQuestion(): Promise<void> {
    if (!ensureLoggedIn()) {
      return;
    }
    loading.value = true;
    try {
      const result = await fetchNextPracticeQuestion({ questionTypes: selectedCategories.value });
      applyResult(result, true);
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, '出题失败'));
    } finally {
      loading.value = false;
    }
  }

  /**
   * 重新回答当前题。
   */
  async function handleRetry(): Promise<void> {
    if (!ensureLoggedIn()) {
      return;
    }
    if (!currentQuestion.value) {
      ElMessage.warning('请先开始一道题');
      return;
    }
    loading.value = true;
    try {
      const result = await retryPracticeQuestion();
      applyResult(result, true);
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, '重新回答失败'));
    } finally {
      loading.value = false;
    }
  }

  /**
   * 发送聊天消息。
   */
  async function sendMessage(): Promise<void> {
    if (!ensureLoggedIn() || loading.value) {
      return;
    }

    // 发送前先锁住当前轮次，避免用户回车打断正在播放的流式回复。
    const content = inputText.value.trim();
    if (!content) {
      ElMessage.warning('请输入内容');
      return;
    }
    inputText.value = '';
    messages.value.push({ id: messageId++, role: 'user', text: content });
    const assistantMessage = appendStreamingAssistantMessage(buildStreamingLoadingText());
    stream.resetStreamTypewriter(assistantMessage);
    snapshot.savePracticeSnapshot();
    snapshot.scrollToBottom(true);
    loading.value = true;
    try {
      await sendPracticeMessageStream(
        { content, questionTypes: selectedCategories.value },
        {
          onMessageChunk: (chunk: string) => stream.appendStreamingChunk(assistantMessage, chunk),
          onResult: (result: PracticeMessageResult) => stream.applyStreamingResult(result, assistantMessage),
        },
      );
      await stream.waitForStreamIdle();
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, '发送失败'));
      stream.preserveInterruptedStreamingMessage(assistantMessage);
    } finally {
      loading.value = false;
    }
  }

  /**
   * 处理游客点击页面功能区的交互。
   */
  function handleGuestInteraction(): void {
    ensureLoggedIn();
  }

  /**
   * 处理游客展开分类下拉框。
   */
  function handleCategoryVisibleChange(visible: boolean): void {
    if (visible) {
      ensureLoggedIn();
    }
  }

  /**
   * 处理分类变更，游客选择后立即回退并提示登录。
   */
  function handleCategoryChange(): void {
    if (authStore.isLoggedIn) {
      return;
    }
    selectedCategories.value = [];
    ensureLoggedIn();
  }

  /**
   * 打开模型授权入口。
   */
  async function handleModelAuthorize(): Promise<void> {
    await openModelAuthorization(modelEntitlementStatus.value);
  }

  /**
   * 校验是否已登录，未登录时弹出统一注册登录提示。
   */
  function ensureLoggedIn(): boolean {
    if (authStore.isLoggedIn) {
      return true;
    }

    // 通过路由查询参数复用布局层登录引导弹窗，保证提示体验统一。
    ElMessage.warning(GUEST_LOGIN_MESSAGE);
    router.replace({ path: route.path, query: { ...route.query, loginGuide: '1' } }).catch(() => undefined);
    return false;
  }

  /**
   * 应用后端返回结果。
   */
  function applyResult(result: PracticeMessageResult, clearMessages: boolean): void {
    phase.value = result.phase;
    currentQuestion.value = result.question || currentQuestion.value;
    growth.value = result.growth;
    syncAuthGrowth(result.growth);

    // 仅出题类消息展示题目卡片，评分和追问回复不再重复复述题目。
    const displayQuestion = result.action === 'QUESTION' ? result.question : null;
    const message = buildAssistantMessage(result.message, displayQuestion, result.grading);
    if (clearMessages) {
      snapshot.clearPracticeSnapshot();
      messages.value = [message];
    } else {
      messages.value.push(message);
    }
    snapshot.savePracticeSnapshot();
    snapshot.scrollToBottom(true);
    showNewBadgeDialog(result);
  }

  /**
   * 完成流式接口最终响应应用。
   */
  function finishStreamingResult(result: PracticeMessageResult, assistantMessage: ChatMessage): void {
    const clearMessages = shouldStartNewQuestion(result);
    phase.value = result.phase;
    currentQuestion.value = result.question || currentQuestion.value;
    growth.value = result.growth;
    syncAuthGrowth(result.growth);

    // 最终事件只补齐业务卡片，正文保持平滑输出后的内容，避免答案突然整段闪现。
    assistantMessage.streaming = false;
    assistantMessage.text = assistantMessage.text || result.message || '';
    assistantMessage.question = result.action === 'QUESTION' ? result.question : null;
    assistantMessage.grading = result.grading;
    if (clearMessages) {
      snapshot.clearPracticeSnapshot();
      messages.value = [assistantMessage];
    }
    refreshMessagesView();
    snapshot.savePracticeSnapshot();
    snapshot.scheduleScrollToBottom();
    showNewBadgeDialog(result);
  }

  /**
   * 新增一个响应式的助手流式消息。
   */
  function appendStreamingAssistantMessage(loadingText: string): ChatMessage {
    const assistantMessage = buildAssistantMessage('', null, null);
    assistantMessage.streaming = true;
    assistantMessage.loadingText = loadingText;
    messages.value.push(assistantMessage);

    // 取回数组中的响应式代理对象，确保后续逐批修改能被 Vue 捕获。
    return messages.value[messages.value.length - 1];
  }

  /**
   * 生成当前轮次的流式加载文案。
   */
  function buildStreamingLoadingText(): string {
    if (phase.value === 'ANSWERING') {
      return 'AI 正在智能评分';
    }

    // 非评分阶段继续使用通用组织答案提示。
    return 'AI 正在组织答案';
  }

  /**
   * 获取流式消息占位文案。
   */
  function streamingPlaceholderText(message: ChatMessage): string {
    return message.loadingText || 'AI 正在组织答案';
  }

  /**
   * 主动刷新消息列表视图。
   */
  function refreshMessagesView(): void {
    // 打字机按字段追加文本，主动触发 ref 更新，避免最后一次状态变化才统一重渲染。
    triggerRef(messages);
  }

  /**
   * 构建助手消息。
   */
  function buildAssistantMessage(text: string, question?: PracticeQuestion | null, grading?: PracticeGrading | null): ChatMessage {
    return { id: messageId++, role: 'assistant', text, question, grading };
  }

  /**
   * 判断是否已经切换到新题。
   */
  function shouldStartNewQuestion(result: PracticeMessageResult): boolean {
    if (result.action !== 'QUESTION' || !result.question) {
      return false;
    }

    // 仅当题目编码发生变化时开启新的聊天记录，重答本题保留上下文。
    return currentQuestion.value?.code !== result.question.code;
  }

  /**
   * 恢复当前题聊天快照。
   */
  function restorePracticeSnapshot(state: PracticeState): void {
    const localSnapshot = snapshot.readPracticeSnapshot();
    if (localSnapshot && localSnapshot.questionCode === state.currentQuestion?.code && hasInterruptedStreamingMessage(localSnapshot.messages)) {
      messages.value = localSnapshot.messages.map(stream.normalizeRestoredMessage);
      messageId = Math.max(...messages.value.map((item) => item.id), 0) + 1;
      snapshot.restoreMessagePanelScroll(localSnapshot.scrollTop, localSnapshot.pinnedToBottom ?? true);
      snapshot.savePracticeSnapshot();
      return;
    }
    if (state.messages.length > 0) {
      messages.value = state.messages.map(toChatMessage);
      messageId = Math.max(...messages.value.map((item) => item.id), 0) + 1;
      snapshot.restoreMessagePanelScroll(localSnapshot?.scrollTop, localSnapshot?.pinnedToBottom ?? true);
      snapshot.savePracticeSnapshot();
      return;
    }
    if (localSnapshot && localSnapshot.questionCode === state.currentQuestion?.code && localSnapshot.messages.length > 0) {
      messages.value = localSnapshot.messages.map(stream.normalizeRestoredMessage);
      messageId = Math.max(...messages.value.map((item) => item.id), 0) + 1;
      snapshot.restoreMessagePanelScroll(localSnapshot.scrollTop, localSnapshot.pinnedToBottom ?? true);
      return;
    }

    // 没有可用快照时，仍然用后端当前题恢复最小可继续状态。
    const text = state.phase === 'DISCUSSING' ? '您可以与我探讨细节、重新作答或者开始下一题。' : '上次还有一道题未完成，请继续作答。';
    messages.value = [buildAssistantMessage(text, state.currentQuestion, null)];
    snapshot.savePracticeSnapshot();
    snapshot.scrollToBottom(true);
  }

  /**
   * 判断本地快照是否包含未完成的流式消息。
   */
  function hasInterruptedStreamingMessage(snapshotMessages: ChatMessage[]): boolean {
    return snapshotMessages.some((item) => item.streaming);
  }

  /**
   * 转换服务端跨端聊天消息。
   */
  function toChatMessage(message: PracticeChatHistoryMessage): ChatMessage {
    return {
      id: messageId++,
      role: message.role,
      text: message.text,
      question: message.question,
      grading: message.grading,
    };
  }

  /**
   * 同步顶部用户成长摘要。
   */
  function syncAuthGrowth(nextGrowth: GrowthInfo): void {
    if (!authStore.user) {
      return;
    }
    authStore.user.experience = nextGrowth.currentExperience;
    authStore.user.level = nextGrowth.level;
    authStore.user.levelName = nextGrowth.levelName;
    authStore.user.rank = nextGrowth.rank;
    authStore.user.levelValue = nextGrowth.levelValue;
    authStore.user.currentLevelExperience = nextGrowth.currentLevelExperience;
    authStore.user.nextLevelExperience = nextGrowth.nextLevelExperience;
    authStore.user.experienceToNextLevel = nextGrowth.experienceToNextLevel;
    authStore.user.levelProgressText = nextGrowth.levelProgressText;
  }

  /**
   * 展示新勋章弹框。
   */
  function showNewBadgeDialog(result: PracticeMessageResult): void {
    const newBadges = extractNewBadges(result);
    if (newBadges.length === 0) {
      return;
    }

    // 弹框打开期间如果又获得新勋章，合并展示而不是连续打断用户。
    const currentBadges = badgeDialogVisible.value ? badgeDialogBadges.value : [];
    badgeDialogBadges.value = mergeUniqueBadges(currentBadges, newBadges);
    badgeDialogVisible.value = true;
  }

  /**
   * 提取并去重新获得的勋章。
   */
  function extractNewBadges(result: PracticeMessageResult): BadgeInfo[] {
    const sourceBadges = [...(result.grading?.newBadges ?? []), ...(result.growth?.newBadges ?? [])];
    return mergeUniqueBadges([], sourceBadges);
  }

  /**
   * 合并并去重新获得的勋章。
   */
  function mergeUniqueBadges(baseBadges: BadgeInfo[], nextBadges: BadgeInfo[]): BadgeInfo[] {
    const badgeKeys = new Set<string>();

    // 同一个勋章可能同时出现在评分结果和成长概览中，前端只提示一次。
    return [...baseBadges, ...nextBadges].filter((badge) => {
      const badgeKey = badge.ruleCode || badge.id;
      if (badgeKeys.has(badgeKey)) {
        return false;
      }
      badgeKeys.add(badgeKey);
      return true;
    });
  }

  /**
   * 关闭勋章弹框后清空临时展示数据。
   */
  function handleBadgeDialogClosed(): void {
    badgeDialogBadges.value = [];
  }

  watch(
    () => authStore.isLoggedIn,
    (isLoggedIn) => {
      if (isLoggedIn) {
        initializePage();
        return;
      }
      stream.clearStreamTypewriter();
      initializeGuestPage().catch((error: unknown) => {
        ElMessage.error(resolveErrorMessage(error, '刷题状态加载失败'));
      });
    },
  );

  onMounted(initializePage);

  onActivated(() => {
    snapshot.restoreMessagePanelScroll();
  });

  onBeforeRouteLeave(() => {
    snapshot.saveMessagePanelStateBeforeLeave();
  });

  onDeactivated(() => {
    snapshot.savePracticeSnapshot();
  });

  onBeforeUnmount(() => {
    snapshot.saveMessagePanelStateBeforeLeave();
    stream.clearStreamTypewriter();
    snapshot.disposePracticeSnapshot();
  });

  return {
    authStore,
    badgeAwardSubtitle,
    badgeDialogBadges,
    badgeDialogVisible,
    categories,
    displayName,
    growth,
    handleBadgeDialogClosed,
    handleCategoryChange,
    handleCategoryVisibleChange,
    handleGuestInteraction,
    handleMessagePanelScroll: snapshot.handleMessagePanelScroll,
    handleModelAuthorize,
    handleNextQuestion,
    handleRetry,
    inputPlaceholder,
    inputText,
    loading,
    messagePanelRef,
    modelEntitlementStatus,
    messages,
    nextButtonText,
    retryButtonDisabled,
    selectedCategories,
    sendMessage,
    streamingPlaceholderText,
  };
}
