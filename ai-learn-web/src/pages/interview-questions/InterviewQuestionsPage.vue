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
          <button type="button" class="markdown-toc-toggle" @click="toggleToc">
            {{ isTocCollapsed ? '展开' : '收起' }}
          </button>
        </div>

        <div v-show="!isTocCollapsed" class="markdown-toc-list interview-category-list">
          <button
            v-for="questionType in questionTypes"
            :key="questionType"
            type="button"
            :class="['markdown-toc-link interview-category-button', { 'is-active': activeQuestionType === questionType }]"
            @click="handleCategoryChange(questionType)"
          >
            {{ questionType }}
          </button>
        </div>
      </nav>

      <article class="markdown-card interview-document-card">
        <el-skeleton :loading="loading" animated :rows="10">
          <el-empty v-if="!activeQuestionType" description="暂无热门面试题内容，管理员补充题库后将自动展示" />
          <div v-else class="markdown-body interview-markdown-body">
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
                <div class="interview-answer" v-html="renderAnswerMarkdown(question.standardAnswer)"></div>
              </article>
            </section>
          </div>
        </el-skeleton>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import MarkdownIt from 'markdown-it';
import { onMounted, ref } from 'vue';
import { fetchInterviewQuestionDocument, fetchPublicQuestionTypes } from '../../api/questions';
import type { QuestionDetail } from '../../types/question';

const activeQuestionType = ref('');
const isTocCollapsed = ref(false);
const loading = ref(false);
const questionDetails = ref<QuestionDetail[]>([]);
const questionTypes = ref<string[]>([]);

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: false,
});

const defaultLinkOpenRenderer = markdown.renderer.rules.link_open;

// 外部链接统一新窗口打开，并避免把来源页面信息带给外部站点。
markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  if (tokens[index].attrIndex('target') < 0) {
    tokens[index].attrPush(['target', '_blank']);
  }

  tokens[index].attrSet('rel', 'noreferrer');
  return defaultLinkOpenRenderer
    ? defaultLinkOpenRenderer(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options);
};

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
 * 渲染参考答案 Markdown。
 */
function renderAnswerMarkdown(value: string | undefined): string {
  return markdown.render(normalizeAnswerMarkdown(value));
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

onMounted(initializeInterviewDocument);
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
  color: #1f6feb;
  background: #f4f8ff;
}

.interview-category-section {
  scroll-margin-top: 88px;
}

.interview-question-card {
  padding: 22px 24px;
  margin: 26px 0 34px;
  background: linear-gradient(135deg, #f8fbff 0%, #f9fffb 100%);
  border: 1px solid #e8eef7;
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
  color: #17233d;
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
  color: #e68a00;
  background: #fff8e8;
  border-color: #ffe1a8;
}

.interview-meta-badge.is-occurrence {
  color: #30a514;
  background: #f1ffe9;
  border-color: #c7efb4;
}

.interview-answer-title {
  margin: 4px 0 10px;
  color: #1f6feb;
  font-size: 15px;
  font-weight: 800;
}

.interview-answer {
  color: #475467;
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
</style>

