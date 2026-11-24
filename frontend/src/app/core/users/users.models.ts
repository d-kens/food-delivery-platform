import { Role, UserStatus } from '../auth/auth.models';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
}

export interface UserQuery {
  status?: UserStatus;
  role?: Role;
  search?: string;
  page?: number;
  size?: number;
}
