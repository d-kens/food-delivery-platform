import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { userResponse } from '../../../testing/users';
import { ChangeEmail } from './change-email';

describe('ChangeEmail', () => {
  let fixture: ComponentFixture<ChangeEmail>;
  let compiled: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChangeEmail],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    TestBed.inject(AuthService).updateSessionUser(userResponse());
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ChangeEmail);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const input = () => compiled.querySelector<HTMLInputElement>('#newEmail')!;
  const button = (text: string) =>
    [...compiled.querySelectorAll('button')].find((b) => b.textContent?.includes(text))!;

  const expand = async () => {
    compiled.querySelector<HTMLButtonElement>('#change-email-toggle')!.click();
    await fixture.whenStable();
  };

  const type = async (value: string) => {
    input().value = value;
    input().dispatchEvent(new Event('input'));
    input().dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  };

  const submit = async () => {
    compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  };

  it('should show the current email and open the form on demand', async () => {
    expect(compiled.textContent).toContain('jane.doe@gmail.com');
    expect(compiled.querySelector('form')).toBeNull();

    await expand();

    expect(document.activeElement).toBe(input());
  });

  it.each([
    ['', 'Enter your new email address'],
    ['not-an-email', 'Enter a valid email address'],
    ['Jane.Doe@Gmail.com', 'This is already your email'],
  ])('should not send %j', async (value, expected) => {
    await expand();
    await type(value);
    await submit();

    expect(compiled.querySelector('#newEmail-message')!.textContent).toContain(expected);
    http.expectNone('/api/user-service/users/me/email');
  });

  it('should request the change and tell the user to check their current inbox', async () => {
    await expand();
    await type(' janet@gmail.com ');
    await submit();

    const request = http.expectOne('/api/user-service/users/me/email');
    expect(request.request.body).toEqual({ newEmail: 'janet@gmail.com' });
    request.flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();

    const status = compiled.querySelector('[role="status"]')!.textContent!;
    expect(status).toContain('jane.doe@gmail.com');
    expect(status).toContain('janet@gmail.com');
    expect(TestBed.inject(AuthService).user()?.email).toBe('jane.doe@gmail.com');

    button('Done').click();
    await fixture.whenStable();
    expect(compiled.querySelector('#change-email-toggle')).not.toBeNull();
  });

  it('should show a taken email above the button and flag the field', async () => {
    await expand();
    await type('taken@gmail.com');
    await submit();

    http
      .expectOne('/api/user-service/users/me/email')
      .flush(
        { title: 'Email already exists', detail: 'An account with this email already exists' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(compiled.querySelector('[role="alert"]')?.textContent).toContain(
      'An account with this email already exists',
    );
    expect(input().getAttribute('aria-invalid')).toBe('true');
  });
});
