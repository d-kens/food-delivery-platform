import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ToastService } from '../../../shared/toast/toast.service';
import { ForgotPassword } from './forgot-password';

describe('ForgotPassword', () => {
  let fixture: ComponentFixture<ForgotPassword>;
  let compiled: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPassword],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ForgotPassword);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const input = () => compiled.querySelector<HTMLInputElement>('#email')!;
  const button = (text: string) =>
    [...compiled.querySelectorAll('button')].find((b) => b.textContent?.includes(text))!;

  const type = async (value: string) => {
    input().value = value;
    input().dispatchEvent(new Event('input'));
    input().dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  };

  const submit = async () => {
    button('Send reset link').click();
    await fixture.whenStable();
  };

  it('should validate the email before sending', async () => {
    await submit();
    expect(compiled.querySelector('#email-message')?.textContent).toContain(
      'Enter your email address',
    );

    await type('not-an-email');
    expect(compiled.querySelector('#email-message')?.textContent).toContain(
      'Enter a valid email address',
    );
    http.expectNone('/auth/forgot-password');
  });

  it('should send the link and show the same confirmation whether or not the account exists', async () => {
    await type(' jane.doe@gmail.com ');
    await submit();

    const request = http.expectOne('/auth/forgot-password');
    expect(request.request.body).toEqual({ email: 'jane.doe@gmail.com' });
    request.flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();

    expect(compiled.textContent).toContain('Check your email');
    expect(compiled.querySelector('[role="status"]')?.textContent).toContain(
      'If an account exists for jane.doe@gmail.com',
    );
  });

  it('should resend the link and confirm with a toast', async () => {
    await type('jane.doe@gmail.com');
    await submit();
    http.expectOne('/auth/forgot-password').flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();

    button('Resend link').click();
    http.expectOne('/auth/forgot-password').flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();

    expect(TestBed.inject(ToastService).toasts()).toEqual([
      expect.objectContaining({
        tone: 'success',
        text: 'We sent another link to jane.doe@gmail.com.',
      }),
    ]);
  });

  it('should go back to the form to use a different email', async () => {
    await type('jane.doe@gmail.com');
    await submit();
    http.expectOne('/auth/forgot-password').flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();

    button('Use a different email').click();
    await fixture.whenStable();

    expect(input().value).toBe('jane.doe@gmail.com');
  });

  it('should explain when the server cannot be reached', async () => {
    await type('jane.doe@gmail.com');
    await submit();

    http.expectOne('/auth/forgot-password').error(new ProgressEvent('error'), { status: 0 });
    await fixture.whenStable();

    expect(compiled.querySelector('[role="alert"]')?.textContent).toContain(
      "Can't reach Keroma right now",
    );
  });

  it('should prefill the email passed from the login page', async () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'forgot-password', component: ForgotPassword }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await TestBed.inject(Router).navigate(['/forgot-password'], {
      state: { email: 'jane.doe@gmail.com' },
    });
    await harness.fixture.whenStable();

    expect(harness.routeNativeElement!.querySelector<HTMLInputElement>('#email')!.value).toBe(
      'jane.doe@gmail.com',
    );
  });
});
