/**
 * 触发浏览器下载 Blob 文件。
 */
export function downloadBlobFile(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();

  // 下载动作触发后立即释放临时 URL，避免长时间占用内存。
  URL.revokeObjectURL(url);
}
