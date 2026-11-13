import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { userResponse } from '../../../testing/users';
import { Profile } from './profile';

describe('Profile', () => {
  let fixture: ComponentFixture<Profile>;
  let compiled: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    TestBed.inject(AuthService).updateSessionUser(userResponse({ phoneNumber: '+254707127309' }));
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Profile);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const input = (id: string) => compiled.querySelector<HTMLInputElement>(`#${id}`);
  const message = (id: string) => compiled.querySelector(`#${id}-message`)!;

  const type = async (id: string, value: string) => {
    input(id)!.value = value;
    input(id)!.dispatchEvent(new Event('input'));
    input(id)!.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  };

  const submit = async () => {
    compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  };

  it('should prefill the form with the current user', () => {
    expect(input('firstName')!.value).toBe('Jane');
    expect(input('lastName')!.value).toBe('Doe');
    expect(input('email')!.value).toBe('jane.doe@gmail.com');
    expect(input('phoneNumber')!.value).toBe('+254707127309');
    expect(input('currentPassword')).toBeNull();
  });

  it('should send only the fields that changed and show a confirmation', async () => {
    await type('firstName', '  Janet ');
    await submit();

    const request = http.expectOne('/users/me');
    expect(request.request.body).toEqual({ firstName: 'Janet' });
    request.flush(userResponse({ firstName: 'Janet', phoneNumber: '+254707127309' }));
    await fixture.whenStable();

    expect(compiled.textContent).toContain('Profile saved.');
    expect(TestBed.inject(AuthService).user()?.firstName).toBe('Janet');
  });

  it('should ask for the current password only when the email changes', async () => {
    await type('email', 'janet@gmail.com');
    expect(input('currentPassword')).not.toBeNull();

    await submit();
    expect(message('currentPassword').textContent).toContain(
      'Enter your current password to change your email',
    );
    http.expectNone('/users/me');

    await type('currentPassword', 'OldPassw0rd!');
    await submit();
    expect(http.expectOne('/users/me').request.body).toEqual({
      email: 'janet@gmail.com',
      currentPassword: 'OldPassw0rd!',
    });
  });

  it('should validate the phone number', async () => {
    await type('phoneNumber', '12ab');

    expect(message('phoneNumber').textContent).toContain('Use 7 to 15 digits');
  });

  it('should show a conflict under the phone number when it is taken', async () => {
    await type('phoneNumber', '+254700000001');
    await submit();

    http
      .expectOne('/users/me')
      .flush(
        {
          title: 'Phone number already exists',
          detail: 'An account with this phone number already exists',
        },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(message('phoneNumber').textContent).toContain(
      'An account with this phone number already exists',
    );
  });

  it('should show a wrong current password under that field', async () => {
    await type('email', 'janet@gmail.com');
    await type('currentPassword', 'wrong');
    await submit();

    http
      .expectOne('/users/me')
      .flush(
        { detail: 'Current password is incorrect' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(message('currentPassword').textContent).toContain('Current password is incorrect');
  });
});
