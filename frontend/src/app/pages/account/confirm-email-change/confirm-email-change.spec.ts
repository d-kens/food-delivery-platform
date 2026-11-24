import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ConfirmEmailChange } from './confirm-email-change';

describe('ConfirmEmailChange', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'account/email/confirm', component: ConfirmEmailChange }]),
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

  const clickConfirm = async () => {
    [...page().querySelectorAll('button')]
      .find((b) => b.textContent?.includes('Confirm email change'))!
      .click();
    await harness.fixture.whenStable();
  };

  const respond = async (body: object | null, status: number, statusText: string) => {
    http.expectOne('/api/user-service/users/me/email/confirm').flush(body, { status, statusText });
    await harness.fixture.whenStable();
  };

  it('should wait for the user to confirm and remove the token from the address bar', async () => {
    await open('/account/email/confirm?token=raw-token');

    expect(TestBed.inject(Location).path()).toBe('/account/email/confirm');
    expect(page().textContent).toContain('Confirm your new email');
    http.expectNone('/api/user-service/users/me/email/confirm');
  });

  it('should send the token when the user confirms', async () => {
    await open('/account/email/confirm?token=raw-token');
    await clickConfirm();

    expect(http.expectOne('/api/user-service/users/me/email/confirm').request.body).toEqual({
      token: 'raw-token',
    });
  });

  it('should show success with a log in link after confirming', async () => {
    await open('/account/email/confirm?token=raw-token');
    await clickConfirm();
    await respond(null, 204, 'No Content');

    expect(page().querySelector('[role="status"]')?.textContent).toContain(
      'Use your new email to log in',
    );
    expect(page().querySelector('a[href="/login"]')).not.toBeNull();
  });

  it('should explain when the new email was taken in the meantime', async () => {
    await open('/account/email/confirm?token=raw-token');
    await clickConfirm();
    await respond(
      { title: 'Email already exists', detail: 'An account with this email already exists' },
      409,
      'Conflict',
    );

    expect(page().querySelector('[role="alert"]')?.textContent).toContain(
      'Request the change again with a different email',
    );
  });

  it('should show the invalid link state when the server rejects the token', async () => {
    await open('/account/email/confirm?token=expired-token');
    await clickConfirm();
    await respond({ detail: 'This link is invalid or has expired' }, 400, 'Bad Request');

    expect(page().textContent).toContain('Your email has not been changed');
  });

  it('should show the invalid link state when there is no token', async () => {
    await open('/account/email/confirm');

    expect(page().textContent).toContain('This link is invalid or has expired');
  });
});
