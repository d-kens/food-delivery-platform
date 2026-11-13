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

    const request = http.expectOne('/users/me');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ firstName: 'Janet' });
    request.flush(userResponse({ firstName: 'Janet' }));

    await result;
    expect(auth.user()?.firstName).toBe('Janet');
  });

  it('should change the password and end the session', async () => {
    const result = firstValueFrom(users.changePassword('OldPassw0rd!', 'NewPassw0rd!'));

    const request = http.expectOne('/users/me/password');
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
      .expectOne('/users/me/password')
      .flush(
        { detail: 'Current password is incorrect' },
        { status: 400, statusText: 'Bad Request' },
      );

    await expect(result).rejects.toBeTruthy();
    expect(auth.isLoggedIn()).toBe(true);
  });

  it('should delete the account with the password in the body and end the session', async () => {
    const result = firstValueFrom(users.deleteAccount('OldPassw0rd!'));

    const request = http.expectOne('/users/me');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body).toEqual({ currentPassword: 'OldPassw0rd!' });
    request.flush(null, { status: 204, statusText: 'No Content' });

    await result;
    expect(auth.isLoggedIn()).toBe(false);
  });

  it('should only send the filters that are set when listing users', () => {
    users.listUsers({ status: 'SUSPENDED', search: '', page: 2, size: 20 }).subscribe();

    const request = http.expectOne((r) => r.url === '/users');
    expect(request.request.params.keys().sort()).toEqual(['page', 'size', 'status']);
    expect(request.request.params.get('status')).toBe('SUSPENDED');
    request.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('should call the admin endpoints', () => {
    users.getUser('user-2').subscribe();
    users.updateStatus('user-2', 'SUSPENDED').subscribe();
    users.updateRoles('user-2', ['USER', 'ADMIN']).subscribe();

    expect(http.expectOne('/users/user-2').request.method).toBe('GET');
    const status = http.expectOne('/users/user-2/status');
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ status: 'SUSPENDED' });
    const roles = http.expectOne('/users/user-2/roles');
    expect(roles.request.method).toBe('PUT');
    expect(roles.request.body).toEqual({ roles: ['USER', 'ADMIN'] });
  });
});
