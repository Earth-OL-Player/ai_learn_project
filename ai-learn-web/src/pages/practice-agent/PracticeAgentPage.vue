<template>
  <section class="practice-chat-page">
    <aside class="practice-side-card">
      <div class="filter-card-title">
        <strong>昨日因,今日果,前尘不咎</strong>
        <strong>今日因,明日果,当下即道</strong>
      </div>

      <RealmCharacterCard
        v-if="growth"
        class="practice-realm-card"
        :nickname="displayName"
        :rank="growth.rank"
        :level="growth.level"
        :current-experience="growth.currentExperience"
        :next-level-experience="growth.nextLevelExperience"
        :level-progress-text="growth.levelProgressText"
        compact
      />

    </aside>

    <main class="practice-chat-card">
      <div class="practice-chat-header">
        <div class="practice-category-picker">
          <span>请选择题目分类</span>
          <el-select v-model="selectedCategories" multiple collapse-tags collapse-tags-tooltip filterable placeholder="全部分类">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </div>
        <el-button class="main-action-button" type="primary" round :loading="loading" @click="handleNextQuestion">{{ nextButtonText }}</el-button>
        <el-button class="sub-action-button" round :disabled="!currentQuestion" :loading="loading" @click="handleRetry">重答本题</el-button>
      </div>

      <section ref="messagePanelRef" class="practice-message-panel">
        <el-empty v-if="messages.length === 0" description="选择分类后点击开始，或直接输入想练习的题型" />
        <article v-for="item in messages" :key="item.id" class="practice-message" :class="item.role">
          <div class="message-bubble">
            <div v-if="item.text" class="message-text message-markdown" v-html="renderMessageText(item)"></div>
            <p v-if="item.streaming && !item.text" class="message-text stream-placeholder">AI 正在思考中...</p>
            <div v-if="item.question" class="question-bubble-card">
              <div class="question-meta-row">
                <el-tag effect="plain">{{ item.question.questionType }}</el-tag>
                <el-tag type="warning" effect="light">重要性 {{ item.question.importanceScore }}</el-tag>
                <el-tag type="success" effect="light">真实面试 {{ item.question.occurrenceCount }} 次</el-tag>
                <el-tag type="info" effect="plain">您的历史最高分 {{ item.question.bestScore }}</el-tag>
              </div>
              <h3>{{ item.question.question }}</h3>
            </div>
            <div v-if="item.grading" class="grading-bubble-card">
              <div class="grading-score-row">
                <strong>{{ item.grading.score }} 分</strong>
                <el-tag :type="item.grading.correct ? 'success' : 'warning'" effect="light">
                  {{ item.grading.correct ? '基本正确' : '继续加油' }}
                </el-tag>
                <el-tag v-if="item.grading.fallbackUsed" type="info" effect="plain">本地兜底评分</el-tag>
                <el-tooltip :content="experienceTooltip(item.grading)" placement="top" effect="light">
                  <div class="experience-float" :class="item.grading.earnedExperience > 0 ? 'gain' : 'same'">
                    <span v-if="item.grading.earnedExperience > 0">↗ +{{ item.grading.earnedExperience }} 经验</span>
                    <span v-else>经验不变</span>
                  </div>
                </el-tooltip>
              </div>
              <p class="grading-advice"><strong>优化建议：</strong>{{ item.grading.improvementAdvice }}</p>
              <el-collapse>
                <el-collapse-item title="查看评分详情" name="detail">
                  <section class="grading-detail-grid">
                    <div>
                      <h4>命中点</h4>
                      <ul class="grading-point-list">
                        <li v-for="(point, index) in normalizeList(item.grading.hitPoints)" :key="`hit-${index}-${point}`">{{ point }}</li>
                      </ul>
                    </div>
                    <div>
                      <h4>缺失点</h4>
                      <ul class="grading-point-list">
                        <li v-for="(point, index) in normalizeList(item.grading.missingPoints)" :key="`missing-${index}-${point}`">{{ point }}</li>
                      </ul>
                    </div>
                    <div>
                      <h4>参考答案</h4>
                      <p>{{ item.grading.referenceAnswer }}</p>
                    </div>
                  </section>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
        </article>
      </section>

      <div class="practice-input-bar">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          resize="none"
          :placeholder="inputPlaceholder"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button type="primary" round :loading="loading" @click="sendMessage">发送</el-button>
      </div>
    </main>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import MarkdownIt from 'markdown-it';
import RealmCharacterCard from '../../components/growth/RealmCharacterCard.vue';
import { computed, nextTick, onMounted, ref } from 'vue';
import {
  fetchNextPracticeQuestion,
  fetchPracticeState,
  retryPracticeQuestion,
  sendPracticeMessageStream,
  type PracticeGrading,
  type PracticeMessageResult,
  type PracticePhase,
  type PracticeQuestion,
  type PracticeState,
} from '../../api/practice';
import type { GrowthInfo } from '../../types/growth';
import { useAuthStore } from '../../stores/auth';

interface ChatMessage {
  id: number;
  role: 'assistant' | 'user';
  text: string;
  question?: PracticeQuestion | null;
  grading?: PracticeGrading | null;
  streaming?: boolean;
}

interface PracticeChatSnapshot {
  phase: PracticePhase;
  questionCode: string;
  messages: ChatMessage[];
  updatedAt: number;
}

const EMPTY_LIST_TEXT = '暂无';
const PRACTICE_CHAT_STORAGE_PREFIX = 'ai_learn_practice_chat_';
const markdownParser = new MarkdownIt({ html: false, breaks: true, linkify: false });
const loading = ref(false);
const inputText = ref('');
const phase = ref<PracticePhase>('QUESTIONING');
const categories = ref<string[]>([]);
const selectedCategories = ref<string[]>([]);
const currentQuestion = ref<PracticeQuestion | null>(null);
const growth = ref<GrowthInfo | null>(null);
const messages = ref<ChatMessage[]>([]);
const messagePanelRef = ref<HTMLElement | null>(null);
const authStore = useAuthStore();
let messageId = 1;

// 左侧角色卡昵称优先使用用户昵称，未设置时回退用户名。
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'AI 学习者');

// 按阶段展示主按钮文案，首次进入是开始，后续是下一题。
const nextButtonText = computed(() => (phase.value === 'QUESTIONING' ? '开始' : '下一题'));

// 输入框提示根据当前阶段变化，引导用户输入最合适的内容。
const inputPlaceholder = computed(() => {
  if (phase.value === 'ANSWERING') {
    return '请输入你的答案';
  }
  if (phase.value === 'DISCUSSING') {
    return '请输入你的疑惑';
  }
  return '请告诉AI你想练的题';
});

/**
 * 初始化刷题页面。
 */
async function initializePage(): Promise<void> {
  loading.value = true;
  try {
    const state = await fetchPracticeState();
    phase.value = state.phase;
    categories.value = state.questionTypes;
    currentQuestion.value = state.currentQuestion;
    growth.value = state.growth;
    syncAuthGrowth(state.growth);
    if (state.currentQuestion) {
      restorePracticeSnapshot(state);
    } else {
      messages.value = [];
      clearPracticeSnapshot();
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '刷题状态加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 抽取下一题。
 */
async function handleNextQuestion(): Promise<void> {
  loading.value = true;
  try {
    const result = await fetchNextPracticeQuestion({ questionTypes: selectedCategories.value });
    applyResult(result, true);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '出题失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 重新回答当前题。
 */
async function handleRetry(): Promise<void> {
  loading.value = true;
  try {
    const result = await retryPracticeQuestion();
    applyResult(result, true);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重新回答失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 发送聊天消息。
 */
async function sendMessage(): Promise<void> {
  const content = inputText.value.trim();
  if (!content) {
    ElMessage.warning('请输入内容');
    return;
  }
  inputText.value = '';
  messages.value.push({ id: messageId++, role: 'user', text: content });
  const assistantMessage = buildAssistantMessage('', null, null);
  assistantMessage.streaming = true;
  messages.value.push(assistantMessage);
  savePracticeSnapshot();
  scrollToBottom();
  loading.value = true;
  try {
    await sendPracticeMessageStream(
      { content, questionTypes: selectedCategories.value },
      {
        onMessageChunk: (chunk: string) => appendStreamingChunk(assistantMessage, chunk),
        onResult: (result: PracticeMessageResult) => applyStreamingResult(result, assistantMessage),
      },
    );
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败');
    removeStreamingMessage(assistantMessage.id);
  } finally {
    loading.value = false;
  }
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
    clearPracticeSnapshot();
    messages.value = [message];
  } else {
    messages.value.push(message);
  }
  savePracticeSnapshot();
  scrollToBottom();
}

/**
 * 应用流式接口的最终响应。
 */
function applyStreamingResult(result: PracticeMessageResult, assistantMessage: ChatMessage): void {
  const clearMessages = shouldStartNewQuestion(result);
  phase.value = result.phase;
  currentQuestion.value = result.question || currentQuestion.value;
  growth.value = result.growth;
  syncAuthGrowth(result.growth);

  // 最终事件携带完整业务数据，用于补齐题目卡片、评分卡片和阶段状态。
  assistantMessage.streaming = false;
  assistantMessage.text = result.message || assistantMessage.text;
  assistantMessage.question = result.action === 'QUESTION' ? result.question : null;
  assistantMessage.grading = result.grading;
  if (clearMessages) {
    clearPracticeSnapshot();
    messages.value = [assistantMessage];
  }
  savePracticeSnapshot();
  scrollToBottom();
}

/**
 * 追加流式文本片段。
 */
function appendStreamingChunk(message: ChatMessage, chunk: string): void {
  message.text += chunk;
  savePracticeSnapshot();
  scrollToBottom();
}

/**
 * 移除异常中断的流式消息。
 */
function removeStreamingMessage(messageIdValue: number): void {
  messages.value = messages.value.filter((item) => item.id !== messageIdValue);
  savePracticeSnapshot();
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
  const snapshot = readPracticeSnapshot();
  if (snapshot && snapshot.questionCode === state.currentQuestion?.code && snapshot.messages.length > 0) {
    messages.value = snapshot.messages;
    messageId = Math.max(...snapshot.messages.map((item) => item.id), 0) + 1;
    return;
  }

  // 没有可用快照时，仍然用后端当前题恢复最小可继续状态。
  const text = state.phase === 'DISCUSSING' ? '您可以与我探讨细节、重新作答或者开始下一题。' : '上次还有一道题未完成，请继续作答。';
  messages.value = [buildAssistantMessage(text, state.currentQuestion, null)];
  savePracticeSnapshot();
}

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
  if (!currentQuestion.value) {
    return;
  }
  const snapshot: PracticeChatSnapshot = {
    phase: phase.value,
    questionCode: currentQuestion.value.code,
    messages: messages.value,
    updatedAt: Date.now(),
  };

  // localStorage 仅保存当前用户当前题轻量对话状态，不保存真实密钥或敏感配置。
  localStorage.setItem(practiceChatStorageKey(), JSON.stringify(snapshot));
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
  return `${PRACTICE_CHAT_STORAGE_PREFIX}${authStore.user?.id ?? 'anonymous'}`;
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
 * 滚动到消息底部。
 */
function scrollToBottom(): void {
  nextTick(() => {
    if (messagePanelRef.value) {
      messagePanelRef.value.scrollTop = messagePanelRef.value.scrollHeight;
    }
  });
}

/**
 * 格式化列表文案。
 */
function normalizeList(values: string[]): string[] {
  return values.length > 0 ? values : [EMPTY_LIST_TEXT];
}

/**
 * 渲染聊天 Markdown 文本。
 */
function renderMessageText(item: ChatMessage): string {
  return markdownParser.render(item.text);
}

/**
 * 生成经验变化悬浮说明。
 */
function experienceTooltip(grading: PracticeGrading): string {
  return grading.experienceDetail || (grading.earnedExperience > 0 ? `比历史最高分多拿了 ${grading.earnedExperience} 分` : '未能突破上次分数');
}

onMounted(initializePage);
</script>

<style scoped lang="scss">
.practice-chat-page {
  // 页面采用左侧筛选、右侧练习的清爽双栏布局。
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 22px;
  min-height: calc(100vh - 150px);
}

.practice-side-card,
.practice-chat-card {
  border: 1px solid rgba(83, 116, 170, 0.1);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 42px rgba(61, 91, 132, 0.08);
}

.practice-side-card {
  align-self: start;
  padding: 20px;
}

.filter-card-title {
  // 顶部使用轻量鼓励语，强化学习陪伴感。
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 124px;
  padding: 22px 24px;
  border: 1px solid rgba(47, 125, 246, 0.08);
  border-radius: 24px;
  background:
    radial-gradient(circle at 90% 14%, rgba(255, 200, 87, 0.22), transparent 28%),
    linear-gradient(135deg, #eef7ff 0%, #f2fff8 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.filter-card-title strong {
  color: #17233d;
  font-family: SimSun, '宋体', serif;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.75;
  white-space: nowrap;
}

.practice-realm-card {
  // 段位角色直接承接左侧引导语，避免重复展示经验卡片。
  margin-top: 18px;
}

.practice-chat-card {
  display: flex;
  min-width: 0;
  overflow: hidden;
  flex-direction: column;
}

.practice-chat-header {
  // 头部集中放置分类选择和高频操作，缩短用户操作路径。
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #edf2f7;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.practice-category-picker {
  display: grid;
  grid-template-columns: auto minmax(220px, 320px);
  gap: 12px;
  align-items: center;
  margin-right: auto;
  padding: 8px 12px 8px 16px;
  border: 1px solid rgba(83, 116, 170, 0.12);
  border-radius: 999px;
  background: rgba(248, 251, 255, 0.86);
}

.practice-category-picker span {
  color: #344054;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.practice-category-picker :deep(.el-select) {
  width: 100%;
}

.practice-category-picker :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 999px;
  box-shadow: none;
}

.main-action-button,
.sub-action-button {
  min-width: 108px;
  font-weight: 700;
}

.practice-message-panel {
  // 对话区留出更大空白，突出刷题内容本身。
  flex: 1;
  min-height: 460px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.practice-message {
  display: flex;
  margin-bottom: 16px;
}

.practice-message.user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(760px, 88%);
  padding: 16px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(61, 91, 132, 0.08);
}

.practice-message.user .message-bubble {
  color: #ffffff;
  background: linear-gradient(135deg, #2f7df6, #35bba8);
}

.message-text,
.question-bubble-card p,
.grading-bubble-card p,
.grading-detail-grid p {
  margin: 0;
  line-height: 1.8;
  white-space: pre-wrap;
}

.message-markdown {
  white-space: normal;
}

.message-markdown :deep(h1),
.message-markdown :deep(h2),
.message-markdown :deep(h3) {
  // 讨论阶段支持 Markdown 标题，间距保持轻量清爽。
  margin: 0 0 10px;
  color: #17233d;
  line-height: 1.45;
}

.practice-message.user .message-markdown :deep(h1),
.practice-message.user .message-markdown :deep(h2),
.practice-message.user .message-markdown :deep(h3),
.practice-message.user .message-markdown :deep(strong) {
  color: #ffffff;
}

.message-markdown :deep(p) {
  margin: 0 0 8px;
  line-height: 1.85;
}

.message-markdown :deep(p:last-child),
.message-markdown :deep(ul:last-child),
.message-markdown :deep(ol:last-child) {
  margin-bottom: 0;
}

.message-markdown :deep(ul),
.message-markdown :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
  line-height: 1.8;
}

.message-markdown :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(47, 125, 246, 0.1);
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.stream-placeholder {
  color: #667085;
}

.question-bubble-card,
.grading-bubble-card {
  position: relative;
  margin-top: 12px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.question-meta-row,
.grading-score-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.question-bubble-card h3 {
  margin: 0 0 12px;
  color: #17233d;
  line-height: 1.7;
}

.grading-score-row strong {
  color: #1f6feb;
  font-size: 28px;
}

.grading-advice {
  color: #344054;
}

.grading-advice strong {
  color: #17233d;
}

.experience-float {
  // 经验变化从详情中移出，用轻量浮层减少对评分内容的干扰。
  margin-left: auto;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(61, 91, 132, 0.12);
}

.experience-float.gain {
  color: #047857;
  background: linear-gradient(135deg, #dcfce7 0%, #f0fdf4 100%);
}

.experience-float.same {
  color: #667085;
  background: linear-gradient(135deg, #f1f5f9 0%, #ffffff 100%);
}

.grading-bubble-card :deep(.el-collapse) {
  // 评分详情与建议文案拉开间距，降低贴边拥挤感。
  margin-top: 12px;
  border-top-color: rgba(83, 116, 170, 0.12);
  border-bottom-color: rgba(83, 116, 170, 0.12);
}

.grading-bubble-card :deep(.el-collapse-item__content) {
  padding: 10px 8px 4px;
}

.grading-detail-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  padding: 4px 2px;
}

.grading-detail-grid > div {
  // 明细块增加内边距和浅色卡片底，阅读时左侧不再贴边。
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid rgba(83, 116, 170, 0.1);
  border-radius: 16px;
  background: #ffffff;
}

.grading-detail-grid h4 {
  margin: 0 0 8px;
  color: #17233d;
}

.grading-point-list {
  // 列表化展示命中点和缺失点，便于用户逐条对照改进。
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
  color: #344054;
  line-height: 1.8;
}

.practice-input-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
  padding: 18px 24px 24px;
  border-top: 1px solid #edf2f7;
}

@media (max-width: 980px) {
  .practice-chat-page {
    grid-template-columns: 1fr;
  }

  .practice-chat-header {
    align-items: stretch;
    flex-direction: column;
    justify-content: stretch;
  }

  .practice-category-picker {
    grid-template-columns: 1fr;
    margin-right: 0;
    border-radius: 18px;
  }

  .main-action-button,
  .sub-action-button {
    flex: 1;
  }

  .practice-input-bar {
    grid-template-columns: 1fr;
  }

  .grading-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
