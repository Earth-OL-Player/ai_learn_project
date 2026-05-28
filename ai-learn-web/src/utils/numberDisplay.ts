/**
 * 格式化数值展示，默认保留一位小数并去除无意义的0。
 *
 * @param value 原始数值
 * @param fractionDigits 保留小数位数
 * @return 展示文本
 */
export function formatNumberDisplay(value: number | string | null | undefined, fractionDigits = 1): string {
  const numericValue = Number(value ?? 0);
  const fixedText = Number.isInteger(numericValue) ? String(numericValue) : numericValue.toFixed(fractionDigits);
  return fixedText.replace(/\.0+$/u, '').replace(/(\.\d*?)0+$/u, '$1');
}

/**
 * 按指定小数位规整数值。
 *
 * @param value 原始数值
 * @param fractionDigits 保留小数位数
 * @return 规整后的数值
 */
export function roundNumber(value: number | string | null | undefined, fractionDigits = 1): number {
  return Number(Number(value ?? 0).toFixed(fractionDigits));
}

/**
 * 将百分比规整到0到100之间。
 *
 * @param value 原始百分比
 * @param fractionDigits 保留小数位数
 * @return 安全百分比
 */
export function clampPercentValue(value: number | string | null | undefined, fractionDigits = 0): number {
  const numericValue = Number(value ?? 0);
  const roundedValue = fractionDigits > 0 ? roundNumber(numericValue, fractionDigits) : Math.round(numericValue);
  return Math.max(0, Math.min(100, roundedValue));
}
