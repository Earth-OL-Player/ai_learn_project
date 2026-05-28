const MEDIUM_DATE_TIME_FORMATTER = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

const RELATIVE_DATE_TIME_FORMATTER = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

const MINUTE_MILLIS = 60 * 1000;
const HOUR_MILLIS = 60 * MINUTE_MILLIS;

/**
 * 格式化管理页常用日期时间。
 */
export function formatMediumDateTime(value: string | null | undefined, fallback = '-'): string {
  const date = parseDate(value);
  return date ? MEDIUM_DATE_TIME_FORMATTER.format(date) : fallback;
}

/**
 * 格式化完整本地日期时间。
 */
export function formatFullDateTime(value: string | null | undefined, fallback = '-'): string {
  const date = parseDate(value);
  return date ? date.toLocaleString('zh-CN', { hour12: false }) : fallback;
}

/**
 * 格式化动态内容流的相对时间。
 */
export function formatRelativeDateTime(value: string | null | undefined, fallback = '刚刚'): string {
  const date = parseDate(value);
  if (!date) {
    return fallback;
  }

  // 一小时内展示相对时间，较早内容展示明确日期和分钟。
  const diffMillis = Date.now() - date.getTime();
  if (diffMillis >= 0 && diffMillis < MINUTE_MILLIS) {
    return '刚刚';
  }
  if (diffMillis >= MINUTE_MILLIS && diffMillis < HOUR_MILLIS) {
    return `${Math.floor(diffMillis / MINUTE_MILLIS)}分钟前`;
  }
  return RELATIVE_DATE_TIME_FORMATTER.format(date);
}

/**
 * 解析接口返回的时间字符串。
 */
function parseDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}
