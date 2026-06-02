import type { RouteLocationNormalizedLoaded } from 'vue-router';

const SITE_NAME = 'AI StudyHub';
const SITE_ORIGIN = 'https://ai-studyhub.cn';
const DEFAULT_TITLE = 'AI StudyHub - AI Agent 应用开发学习平台';
const DEFAULT_DESCRIPTION = 'AI StudyHub 是面向普通开发者的 AI Agent 与 AI 应用开发学习平台，持续整理 Agent学习资料、学习路线、热门面试题、AI 智能刷题和建议社区。';
const DEFAULT_KEYWORDS = 'Agent学习资料,AI Agent学习资料,AI学习平台,AI Agent,AI应用开发,大模型应用开发,RAG,智能刷题,AI面试题,学习路线';
const DEFAULT_IMAGE = `${SITE_ORIGIN}/favicon.svg`;

/**
 * 根据当前路由同步页面 SEO 元信息。
 *
 * @param route 当前已经解析完成的路由
 */
export function updatePageSeo(route: RouteLocationNormalizedLoaded): void {
  const title = resolvePageTitle(route);
  const description = resolveMetaText(route.meta.description, DEFAULT_DESCRIPTION);
  const keywords = resolveMetaText(route.meta.keywords, DEFAULT_KEYWORDS);
  const canonicalUrl = resolveCanonicalUrl(route);
  const robots = route.meta.noIndex ? 'noindex,nofollow' : 'index,follow';

  // 浏览器标题和基础标签优先更新，保证切换路由后搜索摘要信息稳定。
  document.title = title;
  upsertMetaTag('name', 'description', description);
  upsertMetaTag('name', 'keywords', keywords);
  upsertMetaTag('name', 'robots', robots);
  upsertCanonicalLink(canonicalUrl);

  // 开放图谱和 Twitter 摘要保持一致，便于搜索结果和社交分享识别页面。
  upsertMetaTag('property', 'og:title', title);
  upsertMetaTag('property', 'og:description', description);
  upsertMetaTag('property', 'og:url', canonicalUrl);
  upsertMetaTag('property', 'og:image', DEFAULT_IMAGE);
  upsertMetaTag('name', 'twitter:title', title);
  upsertMetaTag('name', 'twitter:description', description);
  updateStructuredData(route, title, description, canonicalUrl);
}

/**
 * 生成适合展示和索引的完整页面标题。
 *
 * @param route 当前路由
 * @return 完整标题
 */
function resolvePageTitle(route: RouteLocationNormalizedLoaded): string {
  const routeTitle = resolveMetaText(route.meta.title, '');
  if (!routeTitle) {
    return DEFAULT_TITLE;
  }
  if (route.name === 'home' || routeTitle.includes(SITE_NAME)) {
    return routeTitle;
  }
  return `${routeTitle} - ${SITE_NAME}`;
}

/**
 * 从路由 meta 中读取文本型 SEO 字段。
 *
 * @param value 路由 meta 字段
 * @param fallback 兜底文本
 * @return 可写入页面标签的文本
 */
function resolveMetaText(value: unknown, fallback: string): string {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback;
}

/**
 * 根据路由生成规范链接地址。
 *
 * @param route 当前路由
 * @return 规范链接
 */
function resolveCanonicalUrl(route: RouteLocationNormalizedLoaded): string {
  const configuredPath = resolveMetaText(route.meta.canonicalPath, '');
  const rawPath = configuredPath || route.path || '/home';
  const normalizedPath = rawPath === '/' ? '/home' : rawPath.replace(/\/+$/u, '');
  return `${SITE_ORIGIN}${normalizedPath || '/home'}`;
}

/**
 * 新增或更新 meta 标签。
 *
 * @param attributeName 标签属性名
 * @param attributeValue 标签属性值
 * @param content 标签内容
 */
function upsertMetaTag(attributeName: 'name' | 'property', attributeValue: string, content: string): void {
  const selector = `meta[${attributeName}="${attributeValue}"]`;
  const meta = document.head.querySelector<HTMLMetaElement>(selector) || document.createElement('meta');

  // 首次创建时补齐标识属性，之后只更新 content。
  if (!meta.parentElement) {
    meta.setAttribute(attributeName, attributeValue);
    document.head.appendChild(meta);
  }
  meta.content = content;
}

/**
 * 新增或更新 canonical 链接。
 *
 * @param href 规范链接地址
 */
function upsertCanonicalLink(href: string): void {
  const link = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]') || document.createElement('link');

  // canonical 链接只有一个，避免重复链接造成搜索引擎误判。
  if (!link.parentElement) {
    link.rel = 'canonical';
    document.head.appendChild(link);
  }
  link.href = href;
}

/**
 * 更新当前页面结构化数据。
 *
 * @param route 当前路由
 * @param title 页面标题
 * @param description 页面描述
 * @param canonicalUrl 规范链接
 */
function updateStructuredData(
  route: RouteLocationNormalizedLoaded,
  title: string,
  description: string,
  canonicalUrl: string
): void {
  const script = (document.getElementById('site-structured-data') as HTMLScriptElement | null)
    || document.createElement('script');
  const structuredData = {
    '@context': 'https://schema.org',
    '@type': route.meta.structuredDataType || 'WebPage',
    name: title,
    url: canonicalUrl,
    inLanguage: 'zh-CN',
    description,
    isPartOf: {
      '@type': 'WebSite',
      name: SITE_NAME,
      url: `${SITE_ORIGIN}/`,
    },
  };

  // JSON.stringify 直接序列化对象，避免手写 JSON 字符串产生转义问题。
  if (!script.parentElement) {
    script.id = 'site-structured-data';
    script.type = 'application/ld+json';
    document.head.appendChild(script);
  }
  script.textContent = JSON.stringify(structuredData);
}
