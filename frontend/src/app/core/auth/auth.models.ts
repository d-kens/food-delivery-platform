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

export interface AuthResponse {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: string[];
}

export interface SessionUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: string[];
}
