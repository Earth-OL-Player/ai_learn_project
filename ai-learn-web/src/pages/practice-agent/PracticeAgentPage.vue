<template>
  <section class="practice-chat-page">
    <aside class="practice-side-card">
      <div class="filter-card-title">
        <strong>一起学习成长吧！</strong>
      </div>

      <div v-if="growth" class="practice-growth-card">
        <span>当前经验</span>
        <strong>{{ growth.currentExperience }}</strong>
        <small>{{ growth.level }} · {{ growth.levelName }} · {{ growth.rank }}</small>
      </div>

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
            <p v-if="item.text" class="message-text">{{ item.text }}</p>
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
              </div>
              <p class="grading-advice">{{ item.grading.improvementAdvice }}</p>
              <el-collapse>
                <el-collapse-item title="查看评分详情" name="detail">
                  <section class="grading-detail-grid">
                    <div>
                      <h4>命中点</h4>
                      <p>{{ formatList(item.grading.hitPoints) }}</p>
                    </div>
                    <div>
                      <h4>缺失点</h4>
                      <p>{{ formatList(item.grading.missingPoints) }}</p>
                    </div>
                    <div>
                      <h4>参考答案</h4>
                      <p>{{ item.grading.referenceAnswer }}</p>
                    </div>
                    <div>
                      <h4>经验变化</h4>
                      <p>本次 +{{ item.grading.earnedExperience }}，当前总经验 {{ item.grading.totalExperience }}</p>
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
import { computed, nextTick, onMounted, ref } from 'vue';
import {
  fetchNextPracticeQuestion,
  fetchPracticeState,
  retryPracticeQuestion,
  sendPracticeMessage,
  type PracticeGrading,
  type PracticeMessageResult,
  type PracticePhase,
  type PracticeQuestion,
} from '../../api/practice';
import type { GrowthInfo } from '../../types/growth';
import { useAuthStore } from '../../stores/auth';

interface ChatMessage {
  id: number;
  role: 'assistant' | 'user';
  text: string;
  question?: PracticeQuestion | null;
  grading?: PracticeGrading | null;
}

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
      messages.value = [buildAssistantMessage(state.phase === 'DISCUSSING' ? '你可以继续追问本题，或点击下一题。' : '上次还有一道题未完成，请继续作答。', state.currentQuestion, null)];
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
  loading.value = true;
  try {
    const result = await sendPracticeMessage({ content, questionTypes: selectedCategories.value });
    applyResult(result, result.action === 'QUESTION');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败');
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
    messages.value = [message];
  } else {
    messages.value.push(message);
  }
  scrollToBottom();
}

/**
 * 构建助手消息。
 */
function buildAssistantMessage(text: string, question?: PracticeQuestion | null, grading?: PracticeGrading | null): ChatMessage {
  return { id: messageId++, role: 'assistant', text, question, grading };
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
function formatList(values: string[]): string {
  return values.length > 0 ? values.join('；') : '暂无';
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
  min-height: 108px;
  padding: 20px;
  border: 1px solid rgba(47, 125, 246, 0.08);
  border-radius: 24px;
  background:
    radial-gradient(circle at 90% 14%, rgba(255, 200, 87, 0.22), transparent 28%),
    linear-gradient(135deg, #eef7ff 0%, #f2fff8 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.filter-card-title strong {
  color: #17233d;
  font-size: 23px;
  letter-spacing: 0.02em;
  line-height: 1.45;
}

.practice-growth-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f7fbff 0%, #f6fff9 100%);
}

.practice-growth-card span,
.practice-growth-card small {
  color: #667085;
}

.practice-growth-card strong {
  color: #1f6feb;
  font-size: 24px;
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
  min-height: 500px;
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

.question-bubble-card,
.grading-bubble-card {
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
