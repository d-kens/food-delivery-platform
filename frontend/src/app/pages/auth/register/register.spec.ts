import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Register } from './register';

describe('Register', () => {
  let fixture: ComponentFixture<Register>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([])],
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
});
