import type { PracticeGrading } from '../../api/practice';
import { createSafeMarkdownRenderer } from '../../utils/safeMarkdown';
import type { ChatMessage } from './types';

const EMPTY_LIST_TEXT = '暂无';
const MARKDOWN_CODE_FENCE_PATTERN = /^\s*(```|~~~)/;
const MARKDOWN_HEADING_WITHOUT_SPACE_PATTERN = /^(#{1,6})([^\s#].*)$/;
const messageMarkdownRenderer = createSafeMarkdownRenderer({ breaks: true, linkify: false });

/**
 * 格式化列表文案。
 */
export function normalizeList(values: string[]): string[] {
  return values.length > 0 ? values : [EMPTY_LIST_TEXT];
}

/**
 * 根据评分生成等级文案。
 */
export function scoreLevelText(score: number): string {
  if (score < 60) {
    return '继续加油';
  }
  if (score < 80) {
    return '合格答案';
  }
  return '非常棒';
}

/**
 * 根据评分生成标签样式。
 */
export function scoreTagType(score: number): 'success' | 'warning' | 'info' {
  if (score < 60) {
    return 'warning';
  }
  return score < 80 ? 'info' : 'success';
}

/**
 * 提取评分问题文案。
 */
export function gradingProblemText(grading: PracticeGrading): string {
  // 过滤空问题点，多个问题点保留模型原有标点并换行展示。
  return grading.problems.map((item) => item.trim()).filter(Boolean).join('\n');
}

/**
 * 提取评分优化建议文案。
 */
export function gradingAdviceText(grading: PracticeGrading): string {
  const adviceText = grading.improvementAdvice.trim();

  // 建议为空时给出稳定占位，保持评分卡片信息完整。
  return adviceText || '暂无优化建议';
}

/**
 * 安全渲染聊天 Markdown 文本。
 */
export function renderSafeMessageText(item: ChatMessage): string {
  return messageMarkdownRenderer.render(normalizeMessageMarkdown(item.text));
}

/**
 * 规范化完整消息中的 Markdown 标题。
 */
function normalizeMessageMarkdown(text: string): string {
  let inCodeFence = false;

  // 逐行处理，避免把代码块里的 #include 或注释误转成标题。
  return text.split('\n').map((line) => {
    if (MARKDOWN_CODE_FENCE_PATTERN.test(line)) {
      inCodeFence = !inCodeFence;
      return line;
    }
    if (inCodeFence) {
      return line;
    }

    // 部分模型会输出“###标题”，补齐空格后交给 markdown-it 正常渲染。
    return line.replace(MARKDOWN_HEADING_WITHOUT_SPACE_PATTERN, '$1 $2');
  }).join('\n');
}

/**
 * 生成经验变化悬浮说明。
 */
export function experienceTooltip(grading: PracticeGrading): string {
  return grading.experienceDetail || (grading.earnedExperience > 0 ? `比历史最高分多拿了 ${grading.earnedExperience} 分` : '未能突破上次分数');
}
