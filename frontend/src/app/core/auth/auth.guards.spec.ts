import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { userResponse } from '../../testing/users';
import { adminGuard, authGuard } from './auth.guards';
import { AuthService } from './auth.service';

@Component({ template: 'page' })
class Page {}

describe('auth guards', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: '', component: Page },
          { path: 'login', component: Page },
          { path: 'account', component: Page, canActivate: [authGuard] },
          { path: 'admin', component: Page, canActivate: [adminGuard] },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  const restoreAs = (body: object | null) => {
    TestBed.inject(AuthService).restoreSession().subscribe();
    const request = http.expectOne((r) => r.url.endsWith('/users/me'));
    if (body) {
      request.flush(body);
    } else {
      request.flush(null, { status: 401, statusText: 'Unauthorized' });
    }
  };

  it('should wait for the session to be restored before deciding', async () => {
    const harness = await RouterTestingHarness.create();
    const navigation = harness.navigateByUrl('/account');

    TestBed.inject(AuthService).restoreSession().subscribe();
    http.expectOne((r) => r.url.endsWith('/users/me')).flush(userResponse());
    await navigation;

    expect(router.url).toBe('/account');
  });

  it('should send logged-out users to login and remember where they were going', async () => {
    const harness = await RouterTestingHarness.create();
    restoreAs(null);
    await harness.navigateByUrl('/account');

    expect(router.url).toBe('/login?next=%2Faccount');
  });

  it('should let admins into admin pages', async () => {
    const harness = await RouterTestingHarness.create();
    restoreAs(userResponse({ roles: ['ADMIN', 'USER'] }));
    await harness.navigateByUrl('/admin');

    expect(router.url).toBe('/admin');
  });

  it('should send non-admins away from admin pages', async () => {
    const harness = await RouterTestingHarness.create();
    restoreAs(userResponse());
    await harness.navigateByUrl('/admin');

    expect(router.url).toBe('/');
  });

  it('should send logged-out users from admin pages to login', async () => {
    const harness = await RouterTestingHarness.create();
    restoreAs(null);
    await harness.navigateByUrl('/admin');

    expect(router.url).toBe('/login?next=%2Fadmin');
  });
});
