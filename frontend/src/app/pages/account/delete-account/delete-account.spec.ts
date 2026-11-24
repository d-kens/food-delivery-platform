import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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

  const button = (text: string) =>
    [...compiled.querySelectorAll('button')].find((b) => b.textContent?.includes(text))!;

  const expand = async () => {
    compiled.querySelector<HTMLButtonElement>('#delete-account-toggle')!.click();
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

  it('should keep the form hidden until the user chooses to delete', async () => {
    expect(compiled.querySelector('form')).toBeNull();

    await expand();

    expect(compiled.querySelector('form')).not.toBeNull();
    expect(compiled.textContent).toContain("This can't be undone.");
    expect(compiled.querySelector('input[type="password"]')).toBeNull();
    expect(document.activeElement).toBe(compiled.querySelector('#confirm'));
  });

  it('should collapse the form on cancel without sending anything', async () => {
    await expand();
    await tickConfirm();

    button('Cancel').click();
    await fixture.whenStable();

    expect(compiled.querySelector('form')).toBeNull();
    http.expectNone('/api/user-service/users/me');
  });

  it('should require the confirmation box', async () => {
    await expand();
    await submit();

    expect(compiled.querySelector('#confirm-message')!.textContent).toContain('Tick the box');
    http.expectNone('/api/user-service/users/me');
  });

  it('should request a confirmation email and keep the user logged in', async () => {
    await expand();
    await tickConfirm();
    await submit();

    const request = http.expectOne('/api/user-service/users/me');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body).toBeNull();
    request.flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();

    expect(compiled.querySelector('[role="status"]')?.textContent).toContain('jane.doe@gmail.com');
    expect(compiled.textContent).toContain("Your account isn't deleted until you click it");
    expect(TestBed.inject(AuthService).isLoggedIn()).toBe(true);

    button('Done').click();
    await fixture.whenStable();
    expect(compiled.querySelector('#delete-account-toggle')).not.toBeNull();
  });

  it('should show a server error above the button', async () => {
    await expand();
    await tickConfirm();
    await submit();

    http
      .expectOne('/api/user-service/users/me')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(compiled.querySelector('[role="alert"]')).not.toBeNull();
    expect(compiled.querySelector('form')).not.toBeNull();
  });
});
