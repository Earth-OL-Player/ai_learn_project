<template>
  <section class="roadmap-page">
    <div :class="['markdown-layout', { 'is-toc-collapsed': isTocCollapsed }]">
      <MarkdownToc
        :items="tocItems"
        :active-id="activeTocId"
        :collapsed="isTocCollapsed"
        list-id="learning-roadmap-toc-list"
        @select="setActiveToc"
        @toggle="toggleToc"
      />

      <article class="markdown-card">
        <div class="markdown-body" v-html="safeRoadmapHtml"></div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import MarkdownToc from '../../components/common/MarkdownToc.vue';
import roadmapMarkdown from '../../content/learning-roadmap/AI应用开发学习路线和资料集.md?raw';
import {
  buildMarkdownTocItems,
  createHeadingIdGenerator,
  installHeadingAnchorRenderer,
  useMarkdownTocNavigation,
} from '../../utils/markdownToc';
import { createSafeMarkdownRenderer } from '../../utils/safeMarkdown';

const {
  activeTocId,
  isTocCollapsed,
  setActiveToc,
  toggleToc,
} = useMarkdownTocNavigation();
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
    installHeadingAnchorRenderer(markdown, (title) => headingIdGenerator(title));
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
 * 安全渲染 Markdown 原文为页面 HTML。
 */
function renderRoadmapMarkdown(): string {
  headingIdGenerator = createHeadingIdGenerator();
  imageCaptionNumber = 0;
  return roadmapMarkdownRenderer.render(roadmapMarkdown);
}

// Markdown 内容由前端项目内 md 文件直接渲染，修改 md 后开发环境会热更新。
const safeRoadmapHtml = computed(() => renderRoadmapMarkdown());
const tocItems = computed(() => buildMarkdownTocItems(roadmapMarkdown));
</script>
