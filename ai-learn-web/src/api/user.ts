import { get, put } from './http';

export type GenderCode = 'MALE' | 'FEMALE';

export interface CurrentUser {
  id: string;
  username: string;
  nickname: string | null;
  avatar: string | null;
  gender: GenderCode | null;
  email: string | null;
  experience: number;
  level: string;
  levelName: string;
  rank: string;
  levelValue: number;
  currentLevelExperience: number;
  nextLevelExperience: number;
  experienceToNextLevel: number;
  levelProgressText: string;
  superAdmin: boolean;
  createdAt: string;
}

export interface UpdateProfilePayload {
  nickname: string;
  gender?: GenderCode | null;
}

/**
 * 查询当前登录用户。
 */
export function getCurrentUser(): Promise<CurrentUser> {
  return get<CurrentUser>('/users/me');
}

/**
 * 更新当前登录用户资料。
 */
export function updateCurrentProfile(payload: UpdateProfilePayload): Promise<CurrentUser> {
  return put<CurrentUser, UpdateProfilePayload>('/users/me/profile', payload);
}
