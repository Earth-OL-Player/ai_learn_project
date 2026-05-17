import { get } from './http';

export interface CurrentUser {
  id: string;
  username: string;
  nickname: string | null;
  avatar: string | null;
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

/**
 * 查询当前登录用户。
 */
export function getCurrentUser(): Promise<CurrentUser> {
  return get<CurrentUser>('/users/me');
}
