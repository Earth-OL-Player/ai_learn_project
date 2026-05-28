<template>
  <section class="roadmap-page home-page">
    <div :class="['markdown-layout', { 'is-toc-collapsed': isTocCollapsed }]">
      <MarkdownToc
        :items="tocItems"
        :active-id="activeTocId"
        :collapsed="isTocCollapsed"
        list-id="home-toc-list"
        @select="setActiveToc"
        @toggle="toggleToc"
      />

      <article class="markdown-card" @click="handleMarkdownClick">
        <div class="markdown-body" v-html="safeHomeHtml"></div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import MarkdownToc from '../../components/common/MarkdownToc.vue';
import homeMarkdown from '../../content/learning-roadmap/首页.md?raw';
import {
  buildMarkdownTocItems,
  createHeadingIdGenerator,
  installHeadingAnchorRenderer,
  useMarkdownTocNavigation,
} from '../../utils/markdownToc';
import { openModelAuthorization } from '../../utils/modelAuthorization';
import { createSafeMarkdownRenderer } from '../../utils/safeMarkdown';

const {
  activeTocId,
  isTocCollapsed,
  setActiveToc,
  toggleToc,
} = useMarkdownTocNavigation();
let headingIdGenerator = createHeadingIdGenerator();

const homeMarkdownRenderer = createSafeMarkdownRenderer({
  linkify: true,
  breaks: false,
  configureMarkdown(markdown) {
    // 本地 Markdown 标题保留锚点能力，渲染结果仍会经过统一消毒。
    installHeadingAnchorRenderer(markdown, (title) => headingIdGenerator(title));
  },
});

/**
 * 安全渲染首页 Markdown 原文为页面 HTML。
 */
function renderHomeMarkdown(): string {
  headingIdGenerator = createHeadingIdGenerator();
  return homeMarkdownRenderer.render(homeMarkdown);
}

/**
 * 处理首页 Markdown 内授权入口点击。
 */
function handleMarkdownClick(event: MouseEvent): void {
  const target = event.target instanceof Element ? event.target : null;
  const authLink = target?.closest<HTMLAnchorElement>('a[href="#model-auth"]');
  if (!authLink) {
    return;
  }

  // 授权入口由后端配置控制，首页只负责发起统一跳转动作。
  event.preventDefault();
  openModelAuthorization().catch(() => undefined);
}

// 首页内容直接读取首页.md，展示方式与路线和资料页保持一致。
const safeHomeHtml = computed(() => renderHomeMarkdown());
const tocItems = computed(() => buildMarkdownTocItems(homeMarkdown));
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
