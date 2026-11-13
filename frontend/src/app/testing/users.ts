import { UserResponse } from '../core/auth/auth.models';

export function userResponse(overrides: Partial<UserResponse> = {}): UserResponse {
  return {
    userId: 'user-1',
    firstName: 'Jane',
    lastName: 'Doe',
    email: 'jane.doe@gmail.com',
    roles: ['USER'],
    phoneNumber: null,
    status: 'ACTIVE',
    ...overrides,
  };
}
