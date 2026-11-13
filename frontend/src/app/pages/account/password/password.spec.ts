import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { userResponse } from '../../../testing/users';
import { Password } from './password';

describe('Password', () => {
  let fixture: ComponentFixture<Password>;
  let compiled: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Password],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    TestBed.inject(AuthService).updateSessionUser(userResponse());
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Password);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const input = (id: string) => compiled.querySelector<HTMLInputElement>(`#${id}`)!;
  const message = (id: string) => compiled.querySelector(`#${id}-message`)!;

  const type = async (id: string, value: string) => {
    input(id).value = value;
    input(id).dispatchEvent(new Event('input'));
    input(id).dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  };

  const submit = async () => {
    compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  };

  it('should require every field', async () => {
    await submit();

    expect(message('currentPassword').textContent).toContain('Enter your current password');
    expect(message('newPassword').textContent).toContain('Enter a new password');
    expect(message('confirmPassword').textContent).toContain('Enter the new password again');
    http.expectNone('/users/me/password');
  });

  it('should reject a short new password', async () => {
    await type('newPassword', 'short');

    expect(message('newPassword').textContent).toContain('Use between 8 and 100 characters');
  });

  it('should not submit when the passwords do not match', async () => {
    await type('currentPassword', 'OldPassw0rd!');
    await type('newPassword', 'NewPassw0rd!');
    await type('confirmPassword', 'Different1!');
    await submit();

    expect(message('confirmPassword').textContent).toContain("Passwords don't match");
    http.expectNone('/users/me/password');
  });

  it('should change the password, end the session and go to login with a notice', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await type('currentPassword', 'OldPassw0rd!');
    await type('newPassword', 'NewPassw0rd!');
    await type('confirmPassword', 'NewPassw0rd!');
    await submit();

    http.expectOne('/users/me/password').flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(TestBed.inject(AuthService).isLoggedIn()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/login', { state: { notice: 'password-changed' } });
  });

  it('should show the server message under the current password when it is wrong', async () => {
    await type('currentPassword', 'wrong');
    await type('newPassword', 'NewPassw0rd!');
    await type('confirmPassword', 'NewPassw0rd!');
    await submit();

    http
      .expectOne('/users/me/password')
      .flush(
        { detail: 'Current password is incorrect' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(message('currentPassword').textContent).toContain('Current password is incorrect');
    expect(TestBed.inject(AuthService).isLoggedIn()).toBe(true);
  });
});
