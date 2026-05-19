<template>
  <section class="roadmap-page home-page">
    <div :class="['markdown-layout', { 'is-toc-collapsed': isTocCollapsed }]">
      <nav
        v-if="tocItems.length"
        :class="['markdown-toc-card', { 'is-collapsed': isTocCollapsed }]"
        aria-label="目录"
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

      <article class="markdown-card">
        <div class="markdown-body" v-html="homeHtml"></div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import MarkdownIt from 'markdown-it';
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import homeMarkdown from '../../content/learning-roadmap/首页.md?raw';

interface TocItem {
  id: string;
  level: number;
  title: string;
}

let tocObserver: IntersectionObserver | null = null;
const activeTocId = ref('');
const isTocCollapsed = ref(false);

const markdown = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: false,
});

let headingIdGenerator = createHeadingIdGenerator();
const defaultLinkOpenRenderer = markdown.renderer.rules.link_open;
const defaultHeadingOpenRenderer = markdown.renderer.rules.heading_open;

markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  const targetIndex = tokens[index].attrIndex('target');
  if (targetIndex < 0) {
    tokens[index].attrPush(['target', '_blank']);
  }

  // 外部链接统一增加安全属性，页面内容仍来自原始 Markdown。
  tokens[index].attrSet('rel', 'noreferrer');
  return defaultLinkOpenRenderer
    ? defaultLinkOpenRenderer(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options);
};

markdown.renderer.rules.heading_open = (tokens, index, options, env, self) => {
  const title = tokens[index + 1]?.content || '';
  const headingId = headingIdGenerator(title);
  tokens[index].attrSet('id', headingId);

  return defaultHeadingOpenRenderer
    ? defaultHeadingOpenRenderer(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options);
};

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
 * 提取 Markdown 标题生成页面目录。
 */
function buildTocItems(markdownText: string): TocItem[] {
  const tocHeadingIdGenerator = createHeadingIdGenerator();
  return markdownText
    .split(/\r?\n/)
    .map((line) => /^(#{2,4})\s+(.+?)\s*#*\s*$/.exec(line))
    .filter((match): match is RegExpExecArray => Boolean(match))
    .map((match) => {
      const title = match[2].replace(/[`*_~]/g, '').trim();
      return {
        id: tocHeadingIdGenerator(title),
        level: match[1].length,
        title,
      };
    });
}

/**
 * 渲染首页 Markdown 原文为页面 HTML。
 */
function renderHomeMarkdown(): string {
  headingIdGenerator = createHeadingIdGenerator();
  return markdown.render(homeMarkdown);
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
 * 监听正文标题位置，自动高亮当前目录。
 */
function observeTocHeadings(): void {
  const headings = Array.from(document.querySelectorAll<HTMLElement>('.markdown-body h2[id], .markdown-body h3[id], .markdown-body h4[id]'));
  if (!headings.length) {
    return;
  }

  activeTocId.value = activeTocId.value || headings[0].id;
  tocObserver = new IntersectionObserver((entries) => {
    const visibleEntry = entries
      .filter((entry) => entry.isIntersecting)
      .sort((first, second) => first.boundingClientRect.top - second.boundingClientRect.top)[0];
    if (visibleEntry?.target.id) {
      activeTocId.value = visibleEntry.target.id;
    }
  }, {
    root: null,
    rootMargin: '-96px 0px -62% 0px',
    threshold: 0,
  });

  headings.forEach((heading) => tocObserver?.observe(heading));
}

// 首页内容直接读取首页.md，展示方式与路线和资料页保持一致。
const homeHtml = computed(() => renderHomeMarkdown());
const tocItems = computed(() => buildTocItems(homeMarkdown));

onMounted(async () => {
  await nextTick();
  observeTocHeadings();
});

onBeforeUnmount(() => {
  tocObserver?.disconnect();
});
</script>

<style scoped>
/* 首页正文标题间距单独放大，避免说明类内容在视觉上过于密集。 */
.home-page :deep(.markdown-body h2:not(:first-child)) {
  margin-top: 58px;
}

/* 三级标题也增加一行左右的呼吸感，但不影响正文列表和段落样式。 */
.home-page :deep(.markdown-body h3) {
  margin-top: 42px;
}
</style>
