export type Role = 'USER' | 'RIDER' | 'ADMIN';

export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'DELETED';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UserResponse {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: Role[];
  phoneNumber: string | null;
  status: UserStatus;
}
