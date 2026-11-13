import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { userResponse } from '../../../testing/users';
import { DeleteAccount } from './delete-account';

describe('DeleteAccount', () => {
  let fixture: ComponentFixture<DeleteAccount>;
  let compiled: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeleteAccount],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    TestBed.inject(AuthService).updateSessionUser(userResponse());
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DeleteAccount);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const message = (id: string) => compiled.querySelector(`#${id}-message`)!;

  const typePassword = async (value: string) => {
    const input = compiled.querySelector<HTMLInputElement>('#currentPassword')!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  };

  const tickConfirm = async () => {
    compiled.querySelector<HTMLInputElement>('#confirm')!.click();
    await fixture.whenStable();
  };

  const submit = async () => {
    compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  };

  it('should warn that deletion cannot be undone', () => {
    expect(compiled.textContent).toContain("This can't be undone.");
  });

  it('should require the password and the confirmation box', async () => {
    await submit();

    expect(message('currentPassword').textContent).toContain('Enter your current password');
    expect(message('confirm').textContent).toContain('Tick the box');
    http.expectNone('/users/me');
  });

  it('should delete the account, end the session and go to login with a notice', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await typePassword('OldPassw0rd!');
    await tickConfirm();
    await submit();

    const request = http.expectOne('/users/me');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body).toEqual({ currentPassword: 'OldPassw0rd!' });
    request.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(TestBed.inject(AuthService).isLoggedIn()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/login', { state: { notice: 'account-deleted' } });
  });

  it('should keep the account and show the error when the password is wrong', async () => {
    await typePassword('wrong');
    await tickConfirm();
    await submit();

    http
      .expectOne('/users/me')
      .flush(
        { detail: 'Current password is incorrect' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(message('currentPassword').textContent).toContain('Current password is incorrect');
    expect(TestBed.inject(AuthService).isLoggedIn()).toBe(true);
  });
});
