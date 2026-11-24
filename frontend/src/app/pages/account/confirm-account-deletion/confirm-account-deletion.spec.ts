import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthService } from '../../../core/auth/auth.service';
import { userResponse } from '../../../testing/users';
import { ConfirmAccountDeletion } from './confirm-account-deletion';

describe('ConfirmAccountDeletion', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'account/delete/confirm', component: ConfirmAccountDeletion }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  const page = () => harness.routeNativeElement!;

  const open = async (url: string) => {
    await harness.navigateByUrl(url);
    await harness.fixture.whenStable();
  };

  const clickDelete = async () => {
    [...page().querySelectorAll('button')]
      .find((b) => b.textContent?.includes('Yes, delete my account'))!
      .click();
    await harness.fixture.whenStable();
  };

  it('should wait for the user to confirm and remove the token from the address bar', async () => {
    await open('/account/delete/confirm?token=raw-token');

    expect(TestBed.inject(Location).path()).toBe('/account/delete/confirm');
    expect(page().textContent).toContain('Delete your account?');
    http.expectNone('/api/user-service/users/me/delete/confirm');
  });

  it('should delete the account, end the session and go to login with a notice', async () => {
    TestBed.inject(AuthService).updateSessionUser(userResponse());
    await open('/account/delete/confirm?token=raw-token');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await clickDelete();

    const request = http.expectOne('/api/user-service/users/me/delete/confirm');
    expect(request.request.body).toEqual({ token: 'raw-token' });
    request.flush(null, { status: 204, statusText: 'No Content' });
    await harness.fixture.whenStable();

    expect(TestBed.inject(AuthService).isLoggedIn()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/login', { state: { notice: 'account-deleted' } });
  });

  it('should show the invalid link state when the server rejects the token', async () => {
    await open('/account/delete/confirm?token=expired-token');
    await clickDelete();

    http
      .expectOne('/api/user-service/users/me/delete/confirm')
      .flush(
        { title: 'Invalid link', detail: 'This link is invalid or has expired' },
        { status: 400, statusText: 'Bad Request' },
      );
    await harness.fixture.whenStable();

    expect(page().textContent).toContain('This link is invalid or has expired');
    expect(page().textContent).toContain('Your account has not been deleted');
  });

  it('should show the invalid link state when there is no token', async () => {
    await open('/account/delete/confirm');

    expect(page().textContent).toContain('This link is invalid or has expired');
  });
});
