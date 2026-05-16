<template>
  <section class="practice-chat-page">
    <aside class="practice-side-card">
      <!-- 侧栏只保留用户需要操作的信息。 -->
      <div class="practice-side-hero">
        <p class="eyebrow">AI 智能刷题</p>
        <h2>专注练习，轻松提升</h2>
        <p class="practice-side-desc">选择练习方向后开始对话，像聊天一样完成出题、作答与复盘。</p>
      </div>

      <el-form label-position="top" class="practice-filter-form">
        <el-form-item label="题目分类">
          <el-select v-model="selectedCategories" multiple collapse-tags collapse-tags-tooltip filterable placeholder="默认全部分类">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <div class="category-summary-row">
          <span>{{ categorySummary }}</span>
          <el-button text type="primary" @click="selectedCategories = []">全部分类</el-button>
        </div>
      </el-form>

      <div v-if="growth" class="practice-growth-card">
        <span class="growth-label">学习成长</span>
        <strong>{{ growth.currentExperience }}</strong>
        <small>{{ growth.level }} · {{ growth.levelName }} · {{ growth.rank }}</small>
      </div>
    </aside>

    <main class="practice-chat-card">
      <div class="practice-chat-header">
        <div class="practice-title-block">
          <span class="chat-badge">实时练习</span>
          <h2>开始你的 AI 刷题</h2>
          <p>出题、作答、评分和追问都在同一个对话窗口完成。</p>
        </div>
        <div class="practice-actions">
          <el-button type="primary" round :loading="loading" @click="handleNextQuestion">{{ nextButtonText }}</el-button>
          <el-button round :disabled="!currentQuestion" :loading="loading" @click="handleRetry">重新回答</el-button>
        </div>
      </div>

      <section ref="messagePanelRef" class="practice-message-panel">
        <el-empty v-if="messages.length === 0" description="选择分类后点击开始刷题，也可以直接输入想练习的方向。" />
        <article v-for="item in messages" :key="item.id" class="practice-message" :class="item.role">
          <div class="message-bubble">
            <p v-if="item.text" class="message-text">{{ item.text }}</p>
            <div v-if="item.question" class="question-bubble-card">
              <div class="question-meta-row">
                <el-tag effect="plain">{{ item.question.questionType }}</el-tag>
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
          placeholder="输入想练习的方向，或直接提交你的答案。"
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

// 按阶段展示按钮文案，首次进入是开始刷题，后续是下一题。
const nextButtonText = computed(() => (phase.value === 'QUESTIONING' ? '开始刷题' : '下一题'));

// 选择摘要用于替代冗长规则说明，保持侧栏信息轻量。
const categorySummary = computed(() => {
  if (selectedCategories.value.length === 0) {
    return '当前：全部分类';
  }
  return `已选择 ${selectedCategories.value.length} 个分类`;
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

  const message = buildAssistantMessage(result.message, result.question, result.grading);
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
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 24px;
  min-height: calc(100vh - 150px);
}

.practice-side-card,
.practice-chat-card {
  border: 1px solid rgba(96, 129, 178, 0.12);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 48px rgba(61, 91, 132, 0.1);
}

.practice-side-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 22px;
  background:
    radial-gradient(circle at 16% 12%, rgba(64, 158, 255, 0.16), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 251, 255, 0.92) 100%);
}

.practice-side-card h2,
.practice-chat-header h2 {
  margin: 0;
  color: #17233d;
}

.practice-side-hero {
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(47, 125, 246, 0.1), rgba(53, 187, 168, 0.1));
}

.practice-side-desc,
.practice-chat-header p {
  color: #667085;
  line-height: 1.8;
}

.practice-filter-form {
  padding: 18px;
  border: 1px solid #edf2f7;
  border-radius: 22px;
  background: #ffffff;
}

.category-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #667085;
  font-size: 14px;
}

.practice-growth-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: auto;
  padding: 20px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(47, 125, 246, 0.1), rgba(53, 187, 168, 0.12)),
    #ffffff;
}

.growth-label,
.practice-growth-card small {
  color: #667085;
}

.practice-growth-card strong {
  color: #1f6feb;
  font-size: 34px;
  line-height: 1;
}

.practice-chat-card {
  display: flex;
  min-width: 0;
  overflow: hidden;
  flex-direction: column;
}

.practice-chat-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 28px 32px;
  border-bottom: 1px solid #edf2f7;
  background:
    radial-gradient(circle at 88% 8%, rgba(47, 125, 246, 0.13), transparent 24%),
    linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
}

.practice-title-block {
  max-width: 620px;
}

.chat-badge {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 5px 12px;
  border-radius: 999px;
  color: #1f6feb;
  font-size: 13px;
  font-weight: 700;
  background: rgba(47, 125, 246, 0.1);
}

.practice-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.practice-message-panel {
  flex: 1;
  min-height: 420px;
  max-height: calc(100vh - 330px);
  overflow-y: auto;
  padding: 30px;
  background:
    radial-gradient(circle at center 36%, rgba(47, 125, 246, 0.05), transparent 24%),
    linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
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
  padding: 18px;
  border: 1px solid rgba(96, 129, 178, 0.08);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(61, 91, 132, 0.08);
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
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
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

.grading-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
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
  padding: 18px 28px 26px;
  border-top: 1px solid #edf2f7;
  background: #ffffff;
}

@media (max-width: 980px) {
  .practice-chat-page {
    grid-template-columns: 1fr;
  }

  .practice-chat-header,
  .practice-input-bar {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .grading-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
