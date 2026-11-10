import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
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

  it('should reserve a message line under every field so errors do not shift the layout', () => {
    expect(message('email')).toBeTruthy();
    expect(message('password')).toBeTruthy();
    expect(errorCount()).toBe(0);
  });

  it('should show an error when leaving an empty required field', async () => {
    input('email').dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    expect(message('email').textContent).toContain('Enter your email address');
    expect(message('password').getAttribute('data-state')).not.toBe('error');
  });

  it('should show required errors and focus the first invalid field on submit', async () => {
    await submit();

    expect(message('email').textContent).toContain('Enter your email address');
    expect(message('password').textContent).toContain('Enter your password');
    expect(input('email').getAttribute('aria-invalid')).toBe('true');
    expect(document.activeElement).toBe(input('email'));
  });

  it('should show an error for an invalid email', async () => {
    await type('email', 'not-an-email');

    expect(message('email').textContent).toContain('Enter a valid email address');
  });

  it('should clear errors as soon as the fields are fixed', async () => {
    await submit();
    await type('email', 'jane.doe@gmail.com');
    await type('password', 'Str0ngPassword!');

    expect(errorCount()).toBe(0);
  });

  it('should warn when Caps Lock is on in the password field', async () => {
    const event = new KeyboardEvent('keydown', { modifierCapsLock: true });
    input('password').dispatchEvent(event);
    await fixture.whenStable();

    expect(message('password').textContent).toContain('Caps Lock is on');
  });

  it('should toggle password visibility', async () => {
    const toggle = compiled.querySelector<HTMLButtonElement>('button[aria-label="Show password"]')!;
    expect(input('password').type).toBe('password');

    toggle.click();
    await fixture.whenStable();

    expect(input('password').type).toBe('text');
    expect(toggle.getAttribute('aria-pressed')).toBe('true');
  });

  describe('talking to the API', () => {
    let http: HttpTestingController;

    beforeEach(() => {
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    const fillValidForm = async () => {
      await type('email', 'jane.doe@gmail.com');
      await type('password', 'Str0ngPassword!');
    };

    it('should log in and go to the page in ?next', async () => {
      const route = TestBed.inject(ActivatedRoute);
      Object.defineProperty(route, 'snapshot', { value: { queryParamMap: convertToParamMap({ next: '/partners' }) } });
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      await fillValidForm();

      await submit();
      expect(compiled.querySelector('button[type="submit"]')?.hasAttribute('disabled')).toBe(true);
      expect(compiled.querySelector('button[type="submit"]')?.textContent).toContain('Logging in');

      http.expectOne((r) => r.url.endsWith('/auth/login')).flush({
        userId: '1', firstName: 'Jane', lastName: 'Doe', email: 'jane.doe@gmail.com', roles: ['USER'],
      });
      await fixture.whenStable();

      expect(navigate).toHaveBeenCalledWith('/partners');
    });

    it('should show "Incorrect email or password" on a 401 and clear it on edit', async () => {
      await fillValidForm();
      await submit();

      http.expectOne((r) => r.url.endsWith('/auth/login')).flush(null, { status: 401, statusText: 'Unauthorized' });
      await fixture.whenStable();

      expect(message('password').textContent).toContain('Incorrect email or password.');
      expect(compiled.querySelector('button[type="submit"]')?.hasAttribute('disabled')).toBe(false);

      await type('password', 'An0therPassword!');
      expect(message('password').textContent).not.toContain('Incorrect email or password.');
    });

    it('should explain when the server cannot be reached', async () => {
      await fillValidForm();
      await submit();

      http.expectOne((r) => r.url.endsWith('/auth/login')).error(new ProgressEvent('error'), { status: 0 });
      await fixture.whenStable();

      expect(message('password').textContent).toContain("Can't reach Keroma right now");
    });
  });

  describe('arriving from sign-up', () => {
    it('should prefill the email and confirm the account was created', async () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [provideRouter([{ path: 'login', component: Login }]), provideHttpClient(), provideHttpClientTesting()],
      });
      const harness = await RouterTestingHarness.create();
      await TestBed.inject(Router).navigate(['/login'], { state: { registeredEmail: 'jane.doe@gmail.com' } });
      await harness.fixture.whenStable();
      const page = harness.routeNativeElement!;

      expect(page.querySelector<HTMLInputElement>('#email')!.value).toBe('jane.doe@gmail.com');
      expect(page.querySelector('[role="status"]')?.textContent).toContain('Account created');
    });

    it('should not show the confirmation on a normal visit', () => {
      expect(compiled.querySelector('[role="status"]')?.textContent ?? '').not.toContain('Account created');
    });
  });
});
