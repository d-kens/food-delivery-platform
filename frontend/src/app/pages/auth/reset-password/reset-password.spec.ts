import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ResetPassword } from './reset-password';

describe('ResetPassword', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'reset-password', component: ResetPassword }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  const page = () => harness.routeNativeElement!;
  const input = (id: string) => page().querySelector<HTMLInputElement>(`#${id}`)!;

  const open = async (url: string) => {
    await harness.navigateByUrl(url);
    await harness.fixture.whenStable();
  };

  const type = async (id: string, value: string) => {
    input(id).value = value;
    input(id).dispatchEvent(new Event('input'));
    input(id).dispatchEvent(new Event('blur'));
    await harness.fixture.whenStable();
  };

  const submitPassword = async (password: string) => {
    await type('newPassword', password);
    await type('confirmPassword', password);
    page().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await harness.fixture.whenStable();
  };

  it('should remove the token from the address bar', async () => {
    await open('/reset-password?token=raw-token');

    expect(TestBed.inject(Location).path()).toBe('/reset-password');
    expect(page().textContent).toContain('Choose a new password');
  });

  it('should reset the password and go to login with a notice', async () => {
    await open('/reset-password?token=raw-token');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await submitPassword('NewPassw0rd!');

    const request = http.expectOne('/auth/reset-password');
    expect(request.request.body).toEqual({ token: 'raw-token', newPassword: 'NewPassw0rd!' });
    request.flush(null, { status: 204, statusText: 'No Content' });
    await harness.fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith('/login', { state: { notice: 'password-reset' } });
  });

  it('should require matching passwords before sending', async () => {
    await open('/reset-password?token=raw-token');
    await type('newPassword', 'NewPassw0rd!');
    await type('confirmPassword', 'Different1!');
    page().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await harness.fixture.whenStable();

    expect(page().querySelector('#confirmPassword-message')?.textContent).toContain(
      "Passwords don't match",
    );
    http.expectNone('/auth/reset-password');
  });

  it('should offer a new link when the server rejects the token', async () => {
    await open('/reset-password?token=expired-token');
    await submitPassword('NewPassw0rd!');

    http
      .expectOne('/auth/reset-password')
      .flush(
        { title: 'Invalid reset link', detail: 'This reset link is invalid or has expired' },
        { status: 400, statusText: 'Bad Request' },
      );
    await harness.fixture.whenStable();

    expect(page().textContent).toContain('This link is invalid or has expired');
    expect(page().querySelector('a[href="/forgot-password"]')?.textContent).toContain(
      'Request a new link',
    );
  });

  it('should show the invalid link state when there is no token', async () => {
    await open('/reset-password');

    expect(page().textContent).toContain('This link is invalid or has expired');
    expect(page().querySelector('form')).toBeNull();
  });
});
