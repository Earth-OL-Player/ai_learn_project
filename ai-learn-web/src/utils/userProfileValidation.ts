// 用户资料前端校验参数与后端用户资料规则保持一致。
export const USER_PROFILE_LIMITS = Object.freeze({
  usernameMaxLength: 32,
  passwordMinLength: 8,
  passwordMaxLength: 64,
  nicknameMaxLength: 64,
  emailMaxLength: 128,
  mottoMaxLength: 60,
});

// 用户资料提示文案集中维护，避免多个表单提示不一致。
export const USER_PROFILE_MESSAGES = Object.freeze({
  usernameInvalid: '用户名仅支持3到32位字母、数字和下划线',
  passwordInvalid: '密码长度需为8到64位',
  createPasswordInvalid: '新增用户密码长度需为8到64位',
  nicknameInvalid: '昵称不能为空，且不能超过64位',
  emailInvalid: '请输入正确的邮箱地址',
  mottoInvalid: '座右铭不能超过60位',
});

// 账号和邮箱格式与后端用户资料校验规则保持同步。
const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,32}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * 校验用户名是否符合后端账号规则。
 */
export function isValidUsername(username: string): boolean {
  return USERNAME_PATTERN.test(username.trim());
}

/**
 * 校验密码长度是否处在允许范围内。
 */
export function isValidPasswordLength(password: string): boolean {
  return password.length >= USER_PROFILE_LIMITS.passwordMinLength
    && password.length <= USER_PROFILE_LIMITS.passwordMaxLength;
}

/**
 * 校验昵称是否非空且不超过最大长度。
 */
export function isValidNickname(nickname: string): boolean {
  const normalizedNickname = nickname.trim();
  return normalizedNickname.length > 0 && normalizedNickname.length <= USER_PROFILE_LIMITS.nicknameMaxLength;
}

/**
 * 校验邮箱是否符合基础格式与长度限制。
 */
export function isValidEmail(email: string): boolean {
  const normalizedEmail = email.trim();
  return normalizedEmail.length <= USER_PROFILE_LIMITS.emailMaxLength && EMAIL_PATTERN.test(normalizedEmail);
}

/**
 * 校验座右铭长度是否符合个人资料限制。
 */
export function isValidMotto(motto: string): boolean {
  return motto.trim().length <= USER_PROFILE_LIMITS.mottoMaxLength;
}
