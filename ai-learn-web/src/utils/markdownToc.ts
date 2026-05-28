import type MarkdownIt from 'markdown-it';
import { nextTick, onBeforeUnmount, onMounted, ref, type Ref } from 'vue';

export interface MarkdownTocItem {
  id: string;
  level: number;
  title: string;
}

interface MarkdownTocNavigation {
  activeTocId: Ref<string>;
  isTocCollapsed: Ref<boolean>;
  setActiveToc: (tocId: string) => void;
  toggleToc: () => void;
}

type HeadingIdGenerator = (title: string) => string;

/**
 * 创建标题锚点生成器，重复标题自动追加序号。
 *
 * @return 标题锚点生成函数
 */
export function createHeadingIdGenerator(): HeadingIdGenerator {
  const headingIdCounter = new Map<string, number>();

  return (title: string) => {
    const baseId = title
      .trim()
      .toLowerCase()
      .replace(/[`*_~()[\]{}]/g, '')
      .replace(/\s+/g, '-')
      .replace(/^-+|-+$/g, '') || 'section';

    // 同一个页面内的重复标题保持稳定递增，避免目录锚点互相覆盖。
    const count = headingIdCounter.get(baseId) || 0;
    headingIdCounter.set(baseId, count + 1);
    return count === 0 ? baseId : `${baseId}-${count + 1}`;
  };
}

/**
 * 提取 Markdown 标题生成页面目录。
 *
 * @param markdownText Markdown 原文
 * @return 页面目录项
 */
export function buildMarkdownTocItems(markdownText: string): MarkdownTocItem[] {
  const tocHeadingIdGenerator = createHeadingIdGenerator();

  // 目录只收集二到四级标题，保持侧边栏层级清晰。
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
 * 为 Markdown 标题补齐可跳转锚点。
 *
 * @param markdown MarkdownIt 渲染实例
 * @param resolveHeadingId 标题锚点生成函数
 */
export function installHeadingAnchorRenderer(markdown: MarkdownIt, resolveHeadingId: HeadingIdGenerator): void {
  const defaultHeadingOpenRenderer = markdown.renderer.rules.heading_open;

  markdown.renderer.rules.heading_open = (tokens, index, options, env, self) => {
    const title = tokens[index + 1]?.content || '';
    const headingId = resolveHeadingId(title);
    tokens[index].attrSet('id', headingId);
    tokens[index].attrSet('tabindex', '-1');

    // 保留 markdown-it 默认标题渲染结果，只追加目录跳转需要的属性。
    return defaultHeadingOpenRenderer
      ? defaultHeadingOpenRenderer(tokens, index, options, env, self)
      : self.renderToken(tokens, index, options);
  };
}

/**
 * 管理 Markdown 目录折叠状态和当前标题高亮。
 *
 * @return 目录交互状态和操作函数
 */
export function useMarkdownTocNavigation(): MarkdownTocNavigation {
  let tocObserver: IntersectionObserver | null = null;
  let mobileViewportMediaQuery: MediaQueryList | null = null;
  const activeTocId = ref('');
  const isTocCollapsed = ref(false);
  const isMobileViewport = ref(false);

  /**
   * 设置当前激活目录。
   *
   * @param tocId 目录锚点ID
   */
  function setActiveToc(tocId: string): void {
    activeTocId.value = tocId;
    if (isMobileViewport.value) {
      isTocCollapsed.value = true;
    }
  }

  /**
   * 切换目录展开和收起状态。
   */
  function toggleToc(): void {
    isTocCollapsed.value = !isTocCollapsed.value;
  }

  /**
   * 同步手机端目录折叠状态。
   *
   * @param event 媒体查询变化事件
   */
  function syncMobileTocState(event?: MediaQueryListEvent): void {
    isMobileViewport.value = event ? event.matches : Boolean(mobileViewportMediaQuery?.matches);
    isTocCollapsed.value = isMobileViewport.value;
  }

  /**
   * 监听正文标题位置，自动高亮当前目录。
   */
  function observeTocHeadings(): void {
    const headings = Array.from(document.querySelectorAll<HTMLElement>('.markdown-body h2[id], .markdown-body h3[id], .markdown-body h4[id]'));
    if (!headings.length) {
      return;
    }

    // 初始进入页面时选中第一个标题，滚动后再由 IntersectionObserver 修正。
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

  onMounted(async () => {
    // 手机端默认收起长目录，让正文更快进入首屏阅读区域。
    mobileViewportMediaQuery = window.matchMedia('(max-width: 768px)');
    syncMobileTocState();
    mobileViewportMediaQuery.addEventListener('change', syncMobileTocState);
    await nextTick();
    observeTocHeadings();
  });

  onBeforeUnmount(() => {
    tocObserver?.disconnect();
    mobileViewportMediaQuery?.removeEventListener('change', syncMobileTocState);
    mobileViewportMediaQuery = null;
  });

  return {
    activeTocId,
    isTocCollapsed,
    setActiveToc,
    toggleToc,
  };
}
