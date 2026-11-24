import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../shared/toast/toast.service';
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
  const alert = () => compiled.querySelector('[role="alert"]');

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
    expect(input('phoneNumber')!.value).toBe('+254707127309');
    expect(input('email')).toBeNull();
    expect(compiled.querySelector('app-change-email')).not.toBeNull();
  });

  it('should enable saving only when something changed, and discard edits', async () => {
    const save = () => compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    const discard = () =>
      [...compiled.querySelectorAll('button')].find((b) => b.textContent?.includes('Discard'))!;
    expect(save().disabled).toBe(true);
    expect(discard().disabled).toBe(true);

    await type('firstName', 'Janet');
    expect(save().disabled).toBe(false);

    discard().click();
    await fixture.whenStable();

    expect(input('firstName')!.value).toBe('Jane');
    expect(save().disabled).toBe(true);
  });

  it('should send only the fields that changed and show a confirmation', async () => {
    await type('firstName', '  Janet ');
    await submit();

    const request = http.expectOne('/api/user-service/users/me');
    expect(request.request.body).toEqual({ firstName: 'Janet' });
    request.flush(userResponse({ firstName: 'Janet', phoneNumber: '+254707127309' }));
    await fixture.whenStable();

    expect(TestBed.inject(ToastService).toasts()).toEqual([
      expect.objectContaining({ tone: 'success', text: 'Profile saved.' }),
    ]);
    expect(TestBed.inject(AuthService).user()?.firstName).toBe('Janet');
  });

  it('should validate the phone number', async () => {
    await type('phoneNumber', '12ab');

    expect(message('phoneNumber').textContent).toContain('Use 7 to 15 digits');
  });

  it('should show a conflict above the button and flag the phone number', async () => {
    await type('phoneNumber', '+254700000001');
    await submit();

    http.expectOne('/api/user-service/users/me').flush(
      {
        title: 'Phone number already exists',
        detail: 'An account with this phone number already exists',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(alert()?.textContent).toContain('An account with this phone number already exists');
    expect(input('phoneNumber')!.getAttribute('aria-invalid')).toBe('true');
  });
});
