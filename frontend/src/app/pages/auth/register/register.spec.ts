import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { Register } from './register';

describe('Register', () => {
  let fixture: ComponentFixture<Register>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  const input = (id: string) => compiled.querySelector<HTMLInputElement>(`#${id}`)!;
  const message = (id: string) => compiled.querySelector(`#${id}-message`)!;
  const errorCount = () => compiled.querySelectorAll('[data-state="error"]').length;

  const submit = async () => {
    compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  };

  const type = async (id: string, value: string) => {
    input(id).value = value;
    input(id).dispatchEvent(new Event('input'));
    input(id).dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  };

  it('should show required errors for every required field and focus the first one', async () => {
    await submit();

    expect(message('firstName').textContent).toContain('Enter your first name');
    expect(message('lastName').textContent).toContain('Enter your last name');
    expect(message('email').textContent).toContain('Enter your email address');
    expect(message('password').textContent).toContain('Create a password');
    expect(document.activeElement).toBe(input('firstName'));
  });

  it('should show an error when leaving an empty required field', async () => {
    input('lastName').dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    expect(message('lastName').textContent).toContain('Enter your last name');
    expect(errorCount()).toBe(1);
  });

  it('should reject a name made only of spaces', async () => {
    await type('firstName', '   ');

    expect(message('firstName').textContent).toContain('Enter your first name');
  });

  it('should show the password rule by the label and an error when it is too short', async () => {
    expect(compiled.querySelector('label[for="password"]')?.textContent).toContain('8+ characters');
    expect(message('password').textContent?.trim()).toBe('');

    await type('password', 'short');

    expect(message('password').getAttribute('data-state')).toBe('error');
    expect(message('password').textContent).toContain('Use at least 8 characters');
  });

  it('should not ask for a phone number', () => {
    expect(compiled.querySelector('#phoneNumber')).toBeNull();
  });

  it('should show no errors when every field is valid', async () => {
    await type('firstName', 'Jane');
    await type('lastName', 'Doe');
    await type('email', 'jane.doe@gmail.com');
    await type('password', 'Str0ngPassword!');
    await submit();

    expect(errorCount()).toBe(0);
  });

  describe('talking to the API', () => {
    let http: HttpTestingController;

    beforeEach(() => {
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    const fillValidForm = async () => {
      await type('firstName', '  Jane ');
      await type('lastName', 'Doe');
      await type('email', 'jane.doe@gmail.com');
      await type('password', 'Str0ngPassword!');
    };

    it('should create the account and go to login with the email, without logging in', async () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      await fillValidForm();
      await submit();

      const register = http.expectOne((r) => r.url.endsWith('/auth/register'));
      expect(register.request.body).toEqual({
        firstName: 'Jane', lastName: 'Doe', email: 'jane.doe@gmail.com', password: 'Str0ngPassword!',
      });
      register.flush({ id: '1' }, { status: 201, statusText: 'Created' });
      await fixture.whenStable();

      http.expectNone((r) => r.url.endsWith('/auth/login'));
      expect(navigate).toHaveBeenCalledWith(['/login'], {
        queryParamsHandling: 'preserve',
        state: { registeredEmail: 'jane.doe@gmail.com' },
      });
    });

    it('should flag the email when it already has an account', async () => {
      await fillValidForm();
      await submit();

      http.expectOne((r) => r.url.endsWith('/auth/register')).flush(
        { title: 'Email already exists', detail: 'An account with this email already exists' },
        { status: 409, statusText: 'Conflict' },
      );
      await fixture.whenStable();

      expect(message('email').textContent).toContain('This email already has an account');
      expect(input('email').getAttribute('aria-invalid')).toBe('true');
      expect(document.activeElement).toBe(input('email'));
    });

    it('should show backend field errors on the matching fields', async () => {
      await fillValidForm();
      await submit();

      http.expectOne((r) => r.url.endsWith('/auth/register')).flush(
        { title: 'Validation failed', errors: { lastName: ['Last name must not exceed 100 characters'] } },
        { status: 400, statusText: 'Bad Request' },
      );
      await fixture.whenStable();

      expect(message('lastName').textContent).toContain('Last name must not exceed 100 characters');
    });
  });
});
