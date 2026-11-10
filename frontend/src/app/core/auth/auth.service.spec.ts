import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AuthResponse } from './auth.models';
import { AuthService, safeRedirect } from './auth.service';

const AUTH_RESPONSE: AuthResponse = {
  userId: 'user-1',
  firstName: 'Jane',
  lastName: 'Doe',
  email: 'jane.doe@gmail.com',
  roles: ['USER'],
};

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should log in and remember the user', async () => {
    const result = firstValueFrom(auth.login({ email: 'jane.doe@gmail.com', password: 'secret123' }));

    const request = http.expectOne((r) => r.url.endsWith('/auth/login'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'jane.doe@gmail.com', password: 'secret123' });
    request.flush(AUTH_RESPONSE);

    expect((await result).firstName).toBe('Jane');
    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.user()?.email).toBe('jane.doe@gmail.com');
  });

  it('should register without logging in', async () => {
    const result = firstValueFrom(
      auth.register({ email: 'jane.doe@gmail.com', password: 'secret123', firstName: 'Jane', lastName: 'Doe' }),
    );

    const request = http.expectOne((r) => r.url.endsWith('/auth/register'));
    expect(request.request.body).toEqual({
      email: 'jane.doe@gmail.com', password: 'secret123', firstName: 'Jane', lastName: 'Doe',
    });
    request.flush({ id: 'user-1' }, { status: 201, statusText: 'Created' });

    await result;
    expect(auth.isLoggedIn()).toBe(false);
    http.expectNone((r) => r.url.endsWith('/auth/login'));
  });

  it('should restore the session from the access cookie without refreshing', async () => {
    const result = firstValueFrom(auth.restoreSession());

    const me = http.expectOne((r) => r.url.endsWith('/users/me'));
    expect(me.request.method).toBe('GET');
    me.flush(AUTH_RESPONSE);

    expect((await result)?.firstName).toBe('Jane');
    expect(auth.isLoggedIn()).toBe(true);
    http.expectNone((r) => r.url.endsWith('/auth/refresh-token'));
  });

  it('should refresh when the access cookie has expired', async () => {
    const result = firstValueFrom(auth.restoreSession());

    http.expectOne((r) => r.url.endsWith('/users/me')).flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne((r) => r.url.endsWith('/auth/refresh-token')).flush(AUTH_RESPONSE);

    expect((await result)?.firstName).toBe('Jane');
    expect(auth.isLoggedIn()).toBe(true);
  });

  it('should resolve to null when neither cookie is valid', async () => {
    const result = firstValueFrom(auth.restoreSession());

    http.expectOne((r) => r.url.endsWith('/users/me')).flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne((r) => r.url.endsWith('/auth/refresh-token')).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(await result).toBeNull();
    expect(auth.isLoggedIn()).toBe(false);
  });

  it('should not try to refresh when the account is suspended', async () => {
    const result = firstValueFrom(auth.restoreSession());

    http.expectOne((r) => r.url.endsWith('/users/me')).flush(null, { status: 403, statusText: 'Forbidden' });

    expect(await result).toBeNull();
    http.expectNone((r) => r.url.endsWith('/auth/refresh-token'));
  });

  it('should send the XSRF token from the cookie on POST requests', async () => {
    document.cookie = 'XSRF-TOKEN=xsrf-from-server; path=/';
    const result = firstValueFrom(auth.restoreSession());

    const me = http.expectOne((r) => r.url.endsWith('/users/me'));
    expect(me.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    me.flush(null, { status: 401, statusText: 'Unauthorized' });

    const refresh = http.expectOne((r) => r.url.endsWith('/auth/refresh-token'));
    expect(refresh.request.headers.get('X-XSRF-TOKEN')).toBe('xsrf-from-server');
    refresh.flush(AUTH_RESPONSE);

    await result;
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });
});

describe('safeRedirect', () => {
  it('should allow in-app paths and reject everything else', () => {
    expect(safeRedirect('/partners/riders/apply')).toBe('/partners/riders/apply');
    expect(safeRedirect('//evil.com')).toBe('/');
    expect(safeRedirect('https://evil.com')).toBe('/');
    expect(safeRedirect(null)).toBe('/');
  });
});
