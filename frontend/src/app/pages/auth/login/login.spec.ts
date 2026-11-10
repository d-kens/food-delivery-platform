import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([])],
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
});
