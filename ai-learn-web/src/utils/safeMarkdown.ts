import DOMPurify, { type Config as DomPurifyConfig } from 'dompurify';
import MarkdownIt, { type Options as MarkdownItOptions } from 'markdown-it';

type MarkdownConfigureCallback = (markdown: MarkdownIt) => void;

interface SafeMarkdownOptions extends Omit<MarkdownItOptions, 'html'> {
  allowHtml?: boolean;
  configureMarkdown?: MarkdownConfigureCallback;
  sanitizeConfig?: DomPurifyConfig;
}

interface SafeMarkdownRenderer {
  markdown: MarkdownIt;
  render: (markdownText: string) => string;
}

// Markdown 渲染只开放业务确实需要的结构化标签，避免任意 HTML 扩散。
const SAFE_MARKDOWN_ALLOWED_TAGS = [
  'a',
  'blockquote',
  'br',
  'code',
  'em',
  'figcaption',
  'figure',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'hr',
  'img',
  'li',
  'ol',
  'p',
  'pre',
  's',
  'strong',
  'table',
  'tbody',
  'td',
  'th',
  'thead',
  'tr',
  'ul',
];

// 仅保留 Markdown 展示所需属性，事件属性和内联样式不会进入页面。
const SAFE_MARKDOWN_ALLOWED_ATTRIBUTES = [
  'alt',
  'class',
  'href',
  'id',
  'decoding',
  'rel',
  'loading',
  'src',
  'target',
  'tabindex',
  'title',
];

// 即使调用方开启原始 HTML，也禁止脚本、嵌入式页面和表单类交互标签。
const SAFE_MARKDOWN_FORBID_TAGS = [
  'button',
  'embed',
  'form',
  'iframe',
  'input',
  'object',
  'script',
  'select',
  'style',
  'textarea',
];

// 内联样式不属于当前 Markdown 业务能力，统一移除降低样式注入风险。
const SAFE_MARKDOWN_FORBID_ATTRIBUTES = ['style'];

/**
 * 创建统一安全 Markdown 渲染器。
 *
 * @param options Markdown 渲染配置和可选的 DOMPurify 消毒配置
 */
export function createSafeMarkdownRenderer(options: SafeMarkdownOptions = {}): SafeMarkdownRenderer {
  const { allowHtml = false, configureMarkdown, sanitizeConfig, ...markdownOptions } = options;
  const markdown = new MarkdownIt({
    ...markdownOptions,
    html: allowHtml,
  });

  // 页面自定义图片、标题等规则后，再统一补齐链接安全属性。
  configureMarkdown?.(markdown);
  configureSafeLinkRenderer(markdown);

  return {
    markdown,
    render: (markdownText: string) => sanitizeHtml(markdown.render(markdownText), sanitizeConfig),
  };
}

/**
 * 统一清洗 Markdown 渲染后的 HTML。
 *
 * @param unsafeHtml 需要消毒的 HTML 字符串
 * @param sanitizeConfig 调用方追加或覆盖的 DOMPurify 配置
 */
export function sanitizeHtml(unsafeHtml: string, sanitizeConfig?: DomPurifyConfig): string {
  return DOMPurify.sanitize(unsafeHtml, buildSanitizeConfig(sanitizeConfig));
}

/**
 * 生成 DOMPurify 白名单配置。
 *
 * @param sanitizeConfig 调用方追加或覆盖的 DOMPurify 配置
 */
function buildSanitizeConfig(sanitizeConfig?: DomPurifyConfig): DomPurifyConfig {
  return {
    ...sanitizeConfig,
    ALLOWED_ATTR: sanitizeConfig?.ALLOWED_ATTR ?? SAFE_MARKDOWN_ALLOWED_ATTRIBUTES,
    ALLOWED_TAGS: sanitizeConfig?.ALLOWED_TAGS ?? SAFE_MARKDOWN_ALLOWED_TAGS,
    FORBID_ATTR: sanitizeConfig?.FORBID_ATTR ?? SAFE_MARKDOWN_FORBID_ATTRIBUTES,
    FORBID_TAGS: sanitizeConfig?.FORBID_TAGS ?? SAFE_MARKDOWN_FORBID_TAGS,
  };
}

/**
 * 为所有 Markdown 链接补齐新窗口和反钓鱼安全属性。
 *
 * @param markdown MarkdownIt 渲染实例
 */
function configureSafeLinkRenderer(markdown: MarkdownIt): void {
  const defaultLinkOpenRenderer = markdown.renderer.rules.link_open;

  markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
    if (tokens[index].attrIndex('target') < 0) {
      tokens[index].attrPush(['target', '_blank']);
    }

    // rel 同时包含 noopener 和 noreferrer，避免新页面反向控制来源页。
    tokens[index].attrSet('rel', 'noopener noreferrer');
    return defaultLinkOpenRenderer
      ? defaultLinkOpenRenderer(tokens, index, options, env, self)
      : self.renderToken(tokens, index, options);
  };
}
