<template>
  <section class="roadmap-page">
    <div :class="['markdown-layout', { 'is-toc-collapsed': isTocCollapsed }]">
      <nav
        v-if="tocItems.length"
        :class="['markdown-toc-card', { 'is-collapsed': isTocCollapsed }]"
        aria-label="目录"
      >
        <div class="markdown-toc-header">
          <h3 v-if="!isTocCollapsed">目录</h3>
          <button
            type="button"
            class="markdown-toc-toggle"
            :aria-expanded="!isTocCollapsed"
            aria-controls="learning-roadmap-toc-list"
            :aria-label="isTocCollapsed ? '展开目录' : '收起目录'"
            @click="toggleToc"
          >
            {{ isTocCollapsed ? '展开' : '收起' }}
          </button>
        </div>

        <div id="learning-roadmap-toc-list" v-show="!isTocCollapsed" class="markdown-toc-list">
          <a
            v-for="item in tocItems"
            :key="item.id"
            :href="`#${item.id}`"
            :class="[
              'markdown-toc-link',
              `markdown-toc-level-${item.level}`,
              { 'is-active': activeTocId === item.id },
            ]"
            :aria-current="activeTocId === item.id ? 'location' : undefined"
            @click="setActiveToc(item.id)"
          >
            {{ item.title }}
          </a>
        </div>
      </nav>

      <article class="markdown-card">
        <div class="markdown-body" v-html="safeRoadmapHtml"></div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import roadmapMarkdown from '../../content/learning-roadmap/AI应用开发学习路线和资料集.md?raw';
import { createSafeMarkdownRenderer } from '../../utils/safeMarkdown';

interface TocItem {
  id: string;
  level: number;
  title: string;
}

let tocObserver: IntersectionObserver | null = null;
const activeTocId = ref('');
const isTocCollapsed = ref(false);
let headingIdGenerator = createHeadingIdGenerator();
let imageCaptionNumber = 0;

const roadmapAssetModules = import.meta.glob(
  '../../content/learning-roadmap/AI应用开发学习路线和资料集.assets/*',
  {
    eager: true,
    import: 'default',
    query: '?url',
  }
) as Record<string, string>;

// 将 Markdown 附带图片资源映射成 Vite 可访问地址。
const roadmapAssetUrlMap = Object.entries(roadmapAssetModules).reduce<Record<string, string>>(
  (assetMap, [assetPath, assetUrl]) => {
    const assetName = assetPath.substring(assetPath.lastIndexOf('/') + 1);
    const relativePath = `AI应用开发学习路线和资料集.assets/${assetName}`;

    // 同时支持原始路径、URL 编码路径和文件名兜底匹配。
    assetMap[relativePath] = assetUrl;
    assetMap[encodeURI(relativePath)] = assetUrl;
    assetMap[assetName] = assetUrl;
    assetMap[encodeURI(assetName)] = assetUrl;
    return assetMap;
  },
  {}
);

const roadmapMarkdownRenderer = createSafeMarkdownRenderer({
  linkify: true,
  breaks: false,
  configureMarkdown(markdown) {
    const defaultImageRenderer = markdown.renderer.rules.image;
    const defaultHeadingOpenRenderer = markdown.renderer.rules.heading_open;

    // 图片资源先解析成 Vite URL，再交给统一消毒链路保留安全标签。
    markdown.renderer.rules.image = (tokens, index, options, env, self) => {
      const source = tokens[index].attrGet('src');
      const resolvedSource = resolveAssetUrl(source);

      if (resolvedSource) {
        tokens[index].attrSet('src', resolvedSource);
      }
      tokens[index].attrSet('loading', 'lazy');
      tokens[index].attrSet('decoding', 'async');

      imageCaptionNumber += 1;
      const imageHtml = defaultImageRenderer
        ? defaultImageRenderer(tokens, index, options, env, self)
        : self.renderToken(tokens, index, options);
      const imageTitle = resolveImageTitle(tokens[index].content, imageCaptionNumber);
      const figureClass = resolveImageFigureClass(tokens[index].content);
      return `<figure class="${figureClass}">${imageHtml}<figcaption>${imageTitle}</figcaption></figure>`;
    };

    // 本地 Markdown 标题保留锚点能力，渲染结果仍会经过统一消毒。
    markdown.renderer.rules.heading_open = (tokens, index, options, env, self) => {
      const title = tokens[index + 1]?.content || '';
      const headingId = headingIdGenerator(title);
      tokens[index].attrSet('id', headingId);
      tokens[index].attrSet('tabindex', '-1');

      return defaultHeadingOpenRenderer
        ? defaultHeadingOpenRenderer(tokens, index, options, env, self)
        : self.renderToken(tokens, index, options);
    };
  },
});

/**
 * 解析 Markdown 本地图片资源地址。
 */
function resolveAssetUrl(source: string | null): string | undefined {
  if (!source || /^https?:\/\//i.test(source)) {
    return undefined;
  }

  // 兼容浏览器编码、Markdown 原始相对路径和文件名匹配。
  const normalizedSource = source.replace(/^\.\//, '');
  const decodedSource = decodeURIComponent(normalizedSource);
  const assetName = decodedSource.substring(decodedSource.lastIndexOf('/') + 1);
  return roadmapAssetUrlMap[normalizedSource]
    || roadmapAssetUrlMap[decodedSource]
    || roadmapAssetUrlMap[assetName]
    || roadmapAssetUrlMap[encodeURI(decodedSource)]
    || roadmapAssetUrlMap[encodeURI(assetName)];
}

/**
 * 根据图片替代文本生成图注。
 */
function resolveImageTitle(altText: string, imageIndex: number): string {
  const safeTitle = roadmapMarkdownRenderer.markdown.utils.escapeHtml(altText.trim() || '图片');
  return `图${imageIndex}-${safeTitle}`;
}

/**
 * 根据图片替代文本生成图片容器样式类。
 */
function resolveImageFigureClass(altText: string): string {
  const normalizedAltText = altText.trim();

  // 首张 AI 应用开发学习路线图单独缩放，避免影响其他资料插图。
  if (normalizedAltText === 'AI应用开发学习路线') {
    return 'markdown-figure is-learning-roadmap-image';
  }

  return 'markdown-figure';
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
 * 安全渲染 Markdown 原文为页面 HTML。
 */
function renderRoadmapMarkdown(): string {
  headingIdGenerator = createHeadingIdGenerator();
  imageCaptionNumber = 0;
  return roadmapMarkdownRenderer.render(roadmapMarkdown);
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

// Markdown 内容由前端项目内 md 文件直接渲染，修改 md 后开发环境会热更新。
const safeRoadmapHtml = computed(() => renderRoadmapMarkdown());
const tocItems = computed(() => buildTocItems(roadmapMarkdown));

onMounted(async () => {
  await nextTick();
  observeTocHeadings();
});

onBeforeUnmount(() => {
  tocObserver?.disconnect();
});
</script>
