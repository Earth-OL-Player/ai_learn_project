export interface UserDisplaySource {
  nickname?: string | null;
  username?: string | null;
}

export const DEFAULT_USER_DISPLAY_NAME = 'AI 学习者';
export const DEFAULT_VISITOR_DISPLAY_NAME = '访客';

/**
 * 解析用户展示名。
 *
 * @param user 用户基础信息
 * @param fallback 无可用名称时的兜底文案
 * @return 展示名
 */
export function resolveUserDisplayName(
  user: UserDisplaySource | null | undefined,
  fallback = DEFAULT_USER_DISPLAY_NAME,
): string {
  // 昵称优先，用户名兜底，避免各页面自行拼接出不一致文案。
  const nickname = user?.nickname?.trim();
  if (nickname) {
    return nickname;
  }

  const username = user?.username?.trim();
  return username || fallback;
}

/**
 * 解析头像默认文字。
 *
 * @param displayName 展示名
 * @param fallback 展示名为空时的兜底文案
 * @return 头像文字
 */
export function resolveAvatarText(displayName: string | null | undefined, fallback = DEFAULT_USER_DISPLAY_NAME): string {
  const safeDisplayName = displayName?.trim() || fallback;
  return safeDisplayName.slice(0, 1).toUpperCase();
}

/**
 * 根据用户基础信息解析头像默认文字。
 *
 * @param user 用户基础信息
 * @param fallback 无可用名称时的兜底文案
 * @return 头像文字
 */
export function resolveUserAvatarText(user: UserDisplaySource | null | undefined, fallback = DEFAULT_USER_DISPLAY_NAME): string {
  return resolveAvatarText(resolveUserDisplayName(user, fallback), fallback);
}
