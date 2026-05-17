<template>
  <section class="roadmap-page interview-document-page">
    <div :class="['markdown-layout', { 'is-toc-collapsed': isTocCollapsed }]">
      <nav
        v-if="tocItems.length"
        :class="['markdown-toc-card', { 'is-collapsed': isTocCollapsed }]"
        aria-label="热门面经目录"
      >
        <div class="markdown-toc-header">
          <h3 v-if="!isTocCollapsed">目录</h3>
          <button type="button" class="markdown-toc-toggle" @click="toggleToc">
            {{ isTocCollapsed ? '展开' : '收起' }}
          </button>
        </div>

        <div v-show="!isTocCollapsed" class="markdown-toc-list">
          <a
            v-for="item in tocItems"
            :key="item.id"
            :href="`#${item.id}`"
            :class="[
              'markdown-toc-link',
              `markdown-toc-level-${item.level}`,
              { 'is-active': activeTocId === item.id },
            ]"
            @click="setActiveToc(item.id)"
          >
            {{ item.title }}
          </a>
        </div>
      </nav>

      <article class="markdown-card interview-document-card">
        <el-skeleton :loading="loading" animated :rows="10">
          <el-empty v-if="categorySections.length === 0" description="暂无热门面经内容，管理员补充题库后将自动展示" />
          <div v-else class="markdown-body interview-markdown-body">
            <h1>热门面经</h1>
            <p class="interview-document-intro">
              精选 AI、RAG、Agent 等方向真实面试高频题，按分类连续阅读，帮助你用文档方式系统复盘。
            </p>

            <section
              v-for="section in categorySections"
              :id="section.id"
              :key="section.id"
              class="interview-category-section"
            >
              <h2>{{ section.title }}</h2>

              <article
                v-for="(question, index) in section.questions"
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
import { ElMessage } from 'element-plus';
import MarkdownIt from 'markdown-it';
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { fetchInterviewQuestionDocument } from '../../api/questions';
import type { QuestionDetail } from '../../types/question';

interface TocItem {
  id: string;
  level: number;
  title: string;
}

interface CategorySection {
  id: string;
  title: string;
  questions: QuestionDetail[];
}

const SCROLL_ACTIVE_OFFSET = 132;

const activeTocId = ref('');
const isTocCollapsed = ref(false);
const loading = ref(false);
const questionDetails = ref<QuestionDetail[]>([]);

let scrollUpdatePending = false;

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

const categorySections = computed(() => buildCategorySections(questionDetails.value));
const tocItems = computed(() => categorySections.value.map(toTocItem));

/**
 * 加载热门面经阅读文档。
 */
async function loadInterviewDocument(): Promise<void> {
  loading.value = true;
  try {
    // 后端已按分类和高频权重排序，前端只负责阅读体验编排。
    questionDetails.value = await fetchInterviewQuestionDocument();
    await nextTick();
    updateActiveTocByReadingPosition();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '热门面经加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 按题目分类构建页面阅读分区。
 */
function buildCategorySections(items: QuestionDetail[]): CategorySection[] {
  const headingIdGenerator = createHeadingIdGenerator();

  // 分类文案为空时兜底到“未分类”，避免正文标题缺失。
  return Array.from(groupQuestionsByType(items).entries()).map(([title, questions]) => ({
    id: headingIdGenerator(title),
    title,
    questions,
  }));
}

/**
 * 按题目分类分组，并保持后端返回顺序。
 */
function groupQuestionsByType(items: QuestionDetail[]): Map<string, QuestionDetail[]> {
  const groupedQuestions = new Map<string, QuestionDetail[]>();

  // 使用 Map 保持接口返回的分类顺序，避免前端二次排序产生跳动。
  items.forEach((item) => {
    const questionTypeText = item.questionTypeText?.trim() || item.questionType?.trim() || '未分类';
    const questions = groupedQuestions.get(questionTypeText) || [];
    questions.push(item);
    groupedQuestions.set(questionTypeText, questions);
  });

  return groupedQuestions;
}

/**
 * 转换阅读分区为左侧目录项。
 */
function toTocItem(section: CategorySection): TocItem {
  return {
    id: section.id,
    level: 2,
    title: section.title,
  };
}

/**
 * 创建标题锚点生成器，重复标题自动追加序号。
 */
function createHeadingIdGenerator(): (title: string) => string {
  const headingIdCounter = new Map<string, number>();

  return (title: string) => {
    const baseId = title
      .trim()
      .toLowerCase()
      .replace(/[`*_~()[\]{}]/g, '')
      .replace(/\s+/g, '-')
      .replace(/^-+|-+$/g, '') || 'section';
    const count = headingIdCounter.get(baseId) || 0;
    headingIdCounter.set(baseId, count + 1);
    return count === 0 ? baseId : `${baseId}-${count + 1}`;
  };
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
 * 设置当前激活目录。
 */
function setActiveToc(tocId: string): void {
  activeTocId.value = tocId;
}

/**
 * 切换目录展开和收起状态。
 */
function toggleToc(): void {
  isTocCollapsed.value = !isTocCollapsed.value;
}

/**
 * 请求根据当前阅读位置刷新目录选中项。
 */
function requestActiveTocUpdate(): void {
  if (scrollUpdatePending) {
    return;
  }

  // 滚动事件较频繁，使用动画帧合并计算避免抖动。
  scrollUpdatePending = true;
  window.requestAnimationFrame(() => {
    scrollUpdatePending = false;
    updateActiveTocByReadingPosition();
  });
}

/**
 * 根据用户当前阅读位置选中左侧目录。
 */
function updateActiveTocByReadingPosition(): void {
  if (!categorySections.value.length) {
    activeTocId.value = '';
    return;
  }

  let currentSectionId = categorySections.value[0].id;
  for (const section of categorySections.value) {
    const sectionElement = document.getElementById(section.id);
    if (!sectionElement) {
      continue;
    }

    // 标题到达顶部导航下方时，即认为用户进入该分类阅读区。
    if (sectionElement.getBoundingClientRect().top <= SCROLL_ACTIVE_OFFSET) {
      currentSectionId = section.id;
      continue;
    }
    break;
  }

  activeTocId.value = currentSectionId;
}

onMounted(async () => {
  await loadInterviewDocument();
  window.addEventListener('scroll', requestActiveTocUpdate, { passive: true });
  window.addEventListener('resize', requestActiveTocUpdate);
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', requestActiveTocUpdate);
  window.removeEventListener('resize', requestActiveTocUpdate);
});
</script>

<style scoped lang="scss">
.interview-document-card {
  min-height: 640px;
}

.interview-document-intro {
  color: #5d6b82;
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
