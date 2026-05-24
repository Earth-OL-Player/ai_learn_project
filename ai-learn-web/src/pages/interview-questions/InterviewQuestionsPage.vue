<template>
  <section class="roadmap-page interview-document-page">
    <div :class="['markdown-layout', { 'is-toc-collapsed': isTocCollapsed }]">
      <nav
        v-if="questionTypes.length"
        :class="['markdown-toc-card interview-category-card', { 'is-collapsed': isTocCollapsed }]"
        aria-label="热门面试题分类"
      >
        <div class="markdown-toc-header">
          <h3 v-if="!isTocCollapsed">分类</h3>
          <button
            type="button"
            class="markdown-toc-toggle"
            :aria-expanded="!isTocCollapsed"
            aria-controls="interview-category-list"
            :aria-label="isTocCollapsed ? '展开热门面试题分类' : '收起热门面试题分类'"
            @click="toggleToc"
          >
            {{ isTocCollapsed ? '展开' : '收起' }}
          </button>
        </div>

        <div id="interview-category-list" v-show="!isTocCollapsed" class="markdown-toc-list interview-category-list">
          <button
            v-for="questionType in questionTypes"
            :key="questionType"
            type="button"
            :class="['markdown-toc-link interview-category-button', { 'is-active': activeQuestionType === questionType }]"
            :aria-pressed="activeQuestionType === questionType"
            @click="handleCategoryChange(questionType)"
          >
            {{ questionType }}
          </button>
        </div>
      </nav>

      <article class="markdown-card interview-document-card">
        <el-skeleton :loading="loading" animated :rows="10">
          <el-empty v-if="!activeQuestionType" description="暂无热门面试题内容，管理员补充题库后将自动展示" />
          <div v-else class="markdown-body interview-markdown-body" aria-live="polite">
            <section class="interview-category-section">
              <h2>{{ activeQuestionType }}</h2>

              <el-empty
                v-if="questionDetails.length === 0"
                :description="`${activeQuestionType} 分类暂无题目，管理员补充题库后将自动展示`"
              />
              <article
                v-for="(question, index) in questionDetails"
                v-else
                :key="question.id"
                class="interview-question-card"
              >
                <div class="interview-question-head">
                  <p class="interview-question-title">题目 {{ index + 1 }}：{{ question.question || '未命名题目' }}</p>
                  <div class="interview-question-meta" aria-label="题目热度信息">
                    <span class="interview-meta-badge is-importance">重要性 {{ formatNumber(question.importanceScore) }}</span>
                    <span class="interview-meta-badge is-occurrence">真实面试 {{ question.occurrenceCount || 0 }} 次</span>
                  </div>
                </div>

                <div class="interview-answer-title">参考答案</div>
                <div class="interview-answer" v-html="renderSafeAnswerMarkdown(question.standardAnswer)"></div>
              </article>
            </section>
          </div>
        </el-skeleton>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElSkeleton } from 'element-plus/es/components/skeleton/index.mjs';
import 'element-plus/es/components/empty/style/css';
import 'element-plus/es/components/skeleton/style/css';
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { fetchInterviewQuestionDocument, fetchPublicQuestionTypes } from '../../api/questions';
import type { QuestionDetail } from '../../types/question';
import { createSafeMarkdownRenderer } from '../../utils/safeMarkdown';

const activeQuestionType = ref('');
const isTocCollapsed = ref(false);
const isMobileViewport = ref(false);
const loading = ref(false);
const questionDetails = ref<QuestionDetail[]>([]);
const questionTypes = ref<string[]>([]);
let mobileViewportMediaQuery: MediaQueryList | null = null;

const answerMarkdownRenderer = createSafeMarkdownRenderer({
  linkify: true,
  breaks: false,
});

/**
 * 初始化热门面试题分类和默认题目。
 */
async function initializeInterviewDocument(): Promise<void> {
  loading.value = true;
  try {
    // 先查询分类，进入页面默认加载第一个分类，避免一次性拉取所有题目。
    questionTypes.value = await fetchPublicQuestionTypes();
    activeQuestionType.value = questionTypes.value[0] || '';
    questionDetails.value = activeQuestionType.value ? await fetchInterviewQuestionDocument(activeQuestionType.value) : [];
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '热门面试题加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 处理分类切换并按分类重新查询题目。
 *
 * @param questionType 目标题目分类
 */
async function handleCategoryChange(questionType: string): Promise<void> {
  if (questionType === activeQuestionType.value || loading.value) {
    return;
  }

  activeQuestionType.value = questionType;
  await loadInterviewDocumentByCategory(questionType);
  if (isMobileViewport.value) {
    isTocCollapsed.value = true;
  }
}

/**
 * 按指定分类加载热门面试题题目。
 *
 * @param questionType 题目分类
 */
async function loadInterviewDocumentByCategory(questionType: string): Promise<void> {
  loading.value = true;
  try {
    // 每次只保留当前分类题目，降低渲染节点数量，缓解页面卡顿。
    questionDetails.value = [];
    questionDetails.value = await fetchInterviewQuestionDocument(questionType);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '热门面试题加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 安全渲染参考答案 Markdown。
 */
function renderSafeAnswerMarkdown(value: string | undefined): string {
  return answerMarkdownRenderer.render(normalizeAnswerMarkdown(value));
}

/**
 * 规整参考答案 Markdown 内容。
 */
function normalizeAnswerMarkdown(value: string | undefined): string {
  if (!value?.trim()) {
    return '暂无参考答案。';
  }

  // 接口数据允许 Markdown 段落和列表，但不让答案内标题抢占分类层级。
  return value.trim().split(/\r?\n/).map(convertAnswerHeadingToBold).join('\n');
}

/**
 * 将答案中的标题行降级为加粗正文。
 */
function convertAnswerHeadingToBold(line: string): string {
  const headingMatch = /^(#{1,6})\s+(.+)$/.exec(line);
  if (!headingMatch) {
    return line;
  }

  return `**${headingMatch[2].trim()}**`;
}

/**
 * 格式化数值展示，去除无意义的小数零。
 */
function formatNumber(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, '');
}

/**
 * 切换目录展开和收起状态。
 */
function toggleToc(): void {
  isTocCollapsed.value = !isTocCollapsed.value;
}

/**
 * 同步手机端分类折叠状态。
 *
 * @param event 媒体查询变化事件
 */
function syncMobileCategoryState(event?: MediaQueryListEvent): void {
  isMobileViewport.value = event ? event.matches : Boolean(mobileViewportMediaQuery?.matches);
  isTocCollapsed.value = isMobileViewport.value;
}

onMounted(() => {
  // 手机端默认收起分类，切换分类后把空间还给题目正文。
  mobileViewportMediaQuery = window.matchMedia('(max-width: 768px)');
  syncMobileCategoryState();
  mobileViewportMediaQuery.addEventListener('change', syncMobileCategoryState);
  void initializeInterviewDocument();
});

onBeforeUnmount(() => {
  mobileViewportMediaQuery?.removeEventListener('change', syncMobileCategoryState);
  mobileViewportMediaQuery = null;
});
</script>

<style scoped lang="scss">
.interview-document-card {
  min-height: 640px;
}

.interview-category-card {
  // 分类导航只承担筛选功能，保持左侧轻量且清爽。
  max-height: calc(100vh - 116px);
}

.interview-category-list {
  gap: 8px;
}

.interview-category-button {
  width: 100%;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.interview-category-button:hover {
  color: var(--color-primary);
  background: var(--color-primary-softer);
}

.interview-category-section {
  scroll-margin-top: 88px;
}

.interview-question-card {
  padding: 22px 24px;
  margin: 26px 0 34px;
  background: linear-gradient(135deg, var(--color-surface-soft) 0%, var(--color-surface) 100%);
  border: 1px solid var(--color-border);
  border-radius: 22px;
}

.interview-question-head {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.interview-question-title {
  flex: 1;
  margin: 0;
  color: var(--color-heading);
  font-size: 21px;
  font-weight: 800;
  line-height: 1.65;
}

.interview-question-meta {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.interview-meta-badge {
  padding: 7px 13px;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  border: 1px solid transparent;
  border-radius: 8px;
}

.interview-meta-badge.is-importance {
  color: #d97706;
  background: color-mix(in srgb, #f59e0b 14%, var(--color-surface));
  border-color: color-mix(in srgb, #f59e0b 34%, var(--color-border));
}

.interview-meta-badge.is-occurrence {
  color: #16a34a;
  background: color-mix(in srgb, #22c55e 14%, var(--color-surface));
  border-color: color-mix(in srgb, #22c55e 34%, var(--color-border));
}

.interview-answer-title {
  margin: 4px 0 10px;
  color: var(--color-primary);
  font-size: 15px;
  font-weight: 800;
}

.interview-answer {
  color: var(--color-text);
}

.interview-answer :deep(p:first-child) {
  margin-top: 0;
}

.interview-answer :deep(p:last-child) {
  margin-bottom: 0;
}

@media (max-width: 1080px) {
  .interview-question-head {
    flex-direction: column;
  }

  .interview-question-meta {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .interview-document-card {
    min-height: 420px;
  }

  .interview-category-card {
    max-height: min(46vh, 340px);
  }

  .interview-question-card {
    padding: 18px 16px;
    margin: 20px 0 26px;
    border-radius: 18px;
  }

  .interview-question-title {
    font-size: 18px;
  }

  .interview-question-meta {
    display: grid;
    width: 100%;
    grid-template-columns: 1fr;
  }

  .interview-meta-badge {
    min-height: 36px;
    align-content: center;
    line-height: 1.3;
  }
}
</style>

