import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { userResponse } from '../../testing/users';
import { AuthService } from '../auth/auth.service';
import { UsersService } from './users.service';

describe('UsersService', () => {
  let users: UsersService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    users = TestBed.inject(UsersService);
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    auth.updateSessionUser(userResponse());
  });

  afterEach(() => http.verify());

  it('should update the profile and refresh the session user', async () => {
    const result = firstValueFrom(users.updateProfile({ firstName: 'Janet' }));

    const request = http.expectOne('/api/user-service/users/me');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ firstName: 'Janet' });
    request.flush(userResponse({ firstName: 'Janet' }));

    await result;
    expect(auth.user()?.firstName).toBe('Janet');
  });

  it('should change the password and end the session', async () => {
    const result = firstValueFrom(users.changePassword('OldPassw0rd!', 'NewPassw0rd!'));

    const request = http.expectOne('/api/user-service/users/me/password');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({
      currentPassword: 'OldPassw0rd!',
      newPassword: 'NewPassw0rd!',
    });
    request.flush(null, { status: 204, statusText: 'No Content' });

    await result;
    expect(auth.isLoggedIn()).toBe(false);
  });

  it('should keep the session when the password change fails', async () => {
    const result = firstValueFrom(users.changePassword('wrong', 'NewPassw0rd!'));
    http
      .expectOne('/api/user-service/users/me/password')
      .flush(
        { detail: 'Current password is incorrect' },
        { status: 400, statusText: 'Bad Request' },
      );

    await expect(result).rejects.toBeTruthy();
    expect(auth.isLoggedIn()).toBe(true);
  });

  it('should request account deletion without a body and keep the session', async () => {
    const result = firstValueFrom(users.requestAccountDeletion());

    const request = http.expectOne('/api/user-service/users/me');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body).toBeNull();
    request.flush(null, { status: 202, statusText: 'Accepted' });

    await result;
    expect(auth.isLoggedIn()).toBe(true);
  });

  it('should confirm account deletion with the token and end the session', async () => {
    const result = firstValueFrom(users.confirmAccountDeletion('raw-token'));

    const request = http.expectOne('/api/user-service/users/me/delete/confirm');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ token: 'raw-token' });
    request.flush(null, { status: 204, statusText: 'No Content' });

    await result;
    expect(auth.isLoggedIn()).toBe(false);
  });

  it('should request an email change', () => {
    users.requestEmailChange('janet@gmail.com').subscribe();

    const request = http.expectOne('/api/user-service/users/me/email');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ newEmail: 'janet@gmail.com' });
    request.flush(null, { status: 202, statusText: 'Accepted' });
  });

  it('should confirm an email change and reload the logged-in user', async () => {
    const result = firstValueFrom(users.confirmEmailChange('raw-token'));

    const confirm = http.expectOne('/api/user-service/users/me/email/confirm');
    expect(confirm.request.body).toEqual({ token: 'raw-token' });
    confirm.flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne('/api/user-service/users/me').flush(userResponse({ email: 'janet@gmail.com' }));

    await result;
    expect(auth.user()?.email).toBe('janet@gmail.com');
  });

  it('should confirm an email change without loading a user when logged out', async () => {
    auth.clearSession();
    const result = firstValueFrom(users.confirmEmailChange('raw-token'));

    http
      .expectOne('/api/user-service/users/me/email/confirm')
      .flush(null, { status: 204, statusText: 'No Content' });

    await result;
    http.expectNone('/api/user-service/users/me');
  });

  it('should only send the filters that are set when listing users', () => {
    users.listUsers({ status: 'SUSPENDED', search: '', page: 2, size: 20 }).subscribe();

    const request = http.expectOne((r) => r.url === '/api/user-service/users');
    expect(request.request.params.keys().sort()).toEqual(['page', 'size', 'status']);
    expect(request.request.params.get('status')).toBe('SUSPENDED');
    request.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('should call the admin endpoints', () => {
    users.getUser('user-2').subscribe();
    users.updateStatus('user-2', 'SUSPENDED').subscribe();
    users.updateRoles('user-2', ['USER', 'ADMIN']).subscribe();

    expect(http.expectOne('/api/user-service/users/user-2').request.method).toBe('GET');
    const status = http.expectOne('/api/user-service/users/user-2/status');
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ status: 'SUSPENDED' });
    const roles = http.expectOne('/api/user-service/users/user-2/roles');
    expect(roles.request.method).toBe('PUT');
    expect(roles.request.body).toEqual({ roles: ['USER', 'ADMIN'] });
  });
});
