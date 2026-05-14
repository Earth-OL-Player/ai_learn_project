import { post } from './http';
import type { CurrentUser } from './user';

export interface RegisterPayload {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface AuthResult {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: CurrentUser;
}

/**
 * 调用注册接口。
 */
export function register(payload: RegisterPayload): Promise<AuthResult> {
  return post<AuthResult, RegisterPayload>('/auth/register', payload);
}

/**
 * 调用登录接口。
 */
export function login(payload: LoginPayload): Promise<AuthResult> {
  return post<AuthResult, LoginPayload>('/auth/login', payload);
}

/**
 * 调用退出登录接口。
 */
export function logout(): Promise<boolean> {
  return post<boolean>('/auth/logout');
}
