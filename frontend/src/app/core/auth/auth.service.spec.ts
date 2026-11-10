import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { authRefreshInterceptor } from './auth-refresh.interceptor';
import { UserResponse } from './auth.models';
import { AuthService, safeRedirect } from './auth.service';

const USER: UserResponse = {
  userId: 'user-1',
  firstName: 'Jane',
  lastName: 'Doe',
  email: 'jane.doe@gmail.com',
  roles: ['USER'],
};

const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let client: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authRefreshInterceptor])), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
  });

  afterEach(() => http.verify());

  const isMe = (r: { url: string }) => r.url.endsWith('/users/me');
  const isRefresh = (r: { url: string }) => r.url.endsWith('/auth/refresh-token');

  it('should log in and remember the user', async () => {
    const result = firstValueFrom(auth.login({ email: 'jane.doe@gmail.com', password: 'secret123' }));

    const request = http.expectOne((r) => r.url.endsWith('/auth/login'));
    expect(request.request.body).toEqual({ email: 'jane.doe@gmail.com', password: 'secret123' });
    request.flush(USER);

    expect((await result).firstName).toBe('Jane');
    expect(auth.isLoggedIn()).toBe(true);
  });

  it('should not try to refresh when login itself is rejected', async () => {
    const result = firstValueFrom(auth.login({ email: 'jane.doe@gmail.com', password: 'wrong' }));

    http.expectOne((r) => r.url.endsWith('/auth/login')).flush(null, UNAUTHORIZED);

    await expect(result).rejects.toBeTruthy();
    http.expectNone(isRefresh);
  });

  it('should register without logging in', async () => {
    const result = firstValueFrom(
      auth.register({ email: 'jane.doe@gmail.com', password: 'secret123', firstName: 'Jane', lastName: 'Doe' }),
    );

    http.expectOne((r) => r.url.endsWith('/auth/register')).flush({ id: 'user-1' }, { status: 201, statusText: 'Created' });

    await result;
    expect(auth.isLoggedIn()).toBe(false);
    http.expectNone((r) => r.url.endsWith('/auth/login'));
  });

  describe('restoring the session', () => {
    it('should use the access cookie without refreshing', async () => {
      const result = firstValueFrom(auth.restoreSession());

      http.expectOne(isMe).flush(USER);

      expect((await result)?.firstName).toBe('Jane');
      http.expectNone(isRefresh);
    });

    it('should refresh and retry when the access cookie has expired', async () => {
      const result = firstValueFrom(auth.restoreSession());

      http.expectOne(isMe).flush(null, UNAUTHORIZED);
      http.expectOne(isRefresh).flush(USER);
      http.expectOne(isMe).flush(USER);

      expect((await result)?.firstName).toBe('Jane');
      expect(auth.isLoggedIn()).toBe(true);
    });

    it('should resolve to null when the refresh token is also invalid', async () => {
      const result = firstValueFrom(auth.restoreSession());

      http.expectOne(isMe).flush(null, UNAUTHORIZED);
      http.expectOne(isRefresh).flush(null, UNAUTHORIZED);

      expect(await result).toBeNull();
      expect(auth.isLoggedIn()).toBe(false);
      http.expectNone(isMe);
    });

    it('should not refresh when the account is suspended', async () => {
      const result = firstValueFrom(auth.restoreSession());

      http.expectOne(isMe).flush(null, { status: 403, statusText: 'Forbidden' });

      expect(await result).toBeNull();
      http.expectNone(isRefresh);
    });
  });

  describe('refresh and retry during a session', () => {
    it('should share one refresh between concurrent 401s and retry both requests', async () => {
      const orders = firstValueFrom(client.get('/api/orders'));
      const cart = firstValueFrom(client.get('/api/cart'));

      http.expectOne('/api/orders').flush(null, UNAUTHORIZED);
      http.expectOne('/api/cart').flush(null, UNAUTHORIZED);
      http.expectOne(isRefresh).flush(USER);
      http.expectOne('/api/orders').flush(['order']);
      http.expectOne('/api/cart').flush(['item']);

      expect(await orders).toEqual(['order']);
      expect(await cart).toEqual(['item']);
    });

    it('should not loop when the retried request is still unauthorized', async () => {
      const orders = firstValueFrom(client.get('/api/orders'));

      http.expectOne('/api/orders').flush(null, UNAUTHORIZED);
      http.expectOne(isRefresh).flush(USER);
      http.expectOne('/api/orders').flush(null, UNAUTHORIZED);

      await expect(orders).rejects.toBeTruthy();
      http.expectNone(isRefresh);
    });

    it('should end the session when refreshing fails', async () => {
      const login = firstValueFrom(auth.login({ email: 'jane.doe@gmail.com', password: 'secret123' }));
      http.expectOne((r) => r.url.endsWith('/auth/login')).flush(USER);
      await login;

      const orders = firstValueFrom(client.get('/api/orders'));
      http.expectOne('/api/orders').flush(null, UNAUTHORIZED);
      http.expectOne(isRefresh).flush(null, UNAUTHORIZED);

      await expect(orders).rejects.toBeTruthy();
      expect(auth.isLoggedIn()).toBe(false);
    });

    it('should send the XSRF token on the refresh request', async () => {
      document.cookie = 'XSRF-TOKEN=xsrf-from-server; path=/';
      const result = firstValueFrom(auth.restoreSession());

      const me = http.expectOne(isMe);
      expect(me.request.headers.has('X-XSRF-TOKEN')).toBe(false);
      me.flush(null, UNAUTHORIZED);

      const refresh = http.expectOne(isRefresh);
      expect(refresh.request.headers.get('X-XSRF-TOKEN')).toBe('xsrf-from-server');
      refresh.flush(USER);
      http.expectOne(isMe).flush(USER);

      await result;
      document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    });
  });

  describe('logging out', () => {
    it('should call the logout endpoint and clear the session', async () => {
      const login = firstValueFrom(auth.login({ email: 'jane.doe@gmail.com', password: 'secret123' }));
      http.expectOne((r) => r.url.endsWith('/auth/login')).flush(USER);
      await login;

      const logout = firstValueFrom(auth.logout(), { defaultValue: undefined });
      const request = http.expectOne((r) => r.url.endsWith('/auth/logout'));
      expect(request.request.method).toBe('POST');
      request.flush(null, { status: 204, statusText: 'No Content' });

      await logout;
      expect(auth.isLoggedIn()).toBe(false);
    });

    it('should clear the session even if the logout request fails', async () => {
      const login = firstValueFrom(auth.login({ email: 'jane.doe@gmail.com', password: 'secret123' }));
      http.expectOne((r) => r.url.endsWith('/auth/login')).flush(USER);
      await login;

      const logout = firstValueFrom(auth.logout(), { defaultValue: undefined });
      http.expectOne((r) => r.url.endsWith('/auth/logout')).error(new ProgressEvent('error'), { status: 0 });

      await logout;
      expect(auth.isLoggedIn()).toBe(false);
    });
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
