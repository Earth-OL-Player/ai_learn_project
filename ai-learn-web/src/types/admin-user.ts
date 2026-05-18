export interface AdminUserItem {
  id: string;
  username: string;
  nickname: string;
  email: string;
  avatar: string | null;
  superAdmin: boolean;
  experience: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserPayload {
  username: string;
  password?: string;
  nickname: string;
  email: string;
  avatar?: string | null;
  superAdmin: boolean;
}

export interface AdminUserQuery {
  pageNo: number;
  pageSize: number;
  keyword?: string;
}

export interface UserLimitInfo {
  maxUsers: number;
  currentUsers: number;
}
