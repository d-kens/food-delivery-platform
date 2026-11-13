import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthService } from '../../../core/auth/auth.service';
import { userResponse } from '../../../testing/users';
import { UserDetail } from './user-detail';

describe('UserDetail', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(
          [{ path: 'admin/users/:id', component: UserDetail }],
          withComponentInputBinding(),
        ),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).updateSessionUser(
      userResponse({ userId: 'admin-1', roles: ['ADMIN'] }),
    );
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  const page = () => harness.routeNativeElement!;
  const button = (text: string) =>
    [...page().querySelectorAll('button')].find((b) => b.textContent?.includes(text));
  const checkbox = (label: string) =>
    [...page().querySelectorAll('label')]
      .find((l) => l.textContent?.includes(label))!
      .querySelector('input')!;

  const open = async (user = userResponse({ userId: 'user-2', phoneNumber: '+254707127309' })) => {
    await harness.navigateByUrl(`/admin/users/${user.userId}`);
    http.expectOne(`/users/${user.userId}`).flush(user);
    await harness.fixture.whenStable();
  };

  it('should show the user details', async () => {
    await open();

    expect(page().textContent).toContain('Jane Doe');
    expect(page().textContent).toContain('jane.doe@gmail.com');
    expect(page().textContent).toContain('+254707127309');
    expect(page().textContent).toContain('Active');
  });

  it('should show not found for a missing user', async () => {
    await harness.navigateByUrl('/admin/users/missing');
    http
      .expectOne('/users/missing')
      .flush({ title: 'User Not Found' }, { status: 404, statusText: 'Not Found' });
    await harness.fixture.whenStable();

    expect(page().textContent).toContain("This user doesn't exist.");
  });

  it('should suspend an active user', async () => {
    await open();
    button('Suspend user')!.click();

    const request = http.expectOne('/users/user-2/status');
    expect(request.request.body).toEqual({ status: 'SUSPENDED' });
    request.flush(null, { status: 204, statusText: 'No Content' });
    await harness.fixture.whenStable();

    expect(page().textContent).toContain('Suspended');
    expect(page().textContent).toContain('User suspended');
    expect(button('Reactivate user')).toBeTruthy();
  });

  it('should reactivate a suspended user', async () => {
    await open(userResponse({ userId: 'user-2', status: 'SUSPENDED' }));
    button('Reactivate user')!.click();

    const request = http.expectOne('/users/user-2/status');
    expect(request.request.body).toEqual({ status: 'ACTIVE' });
    request.flush(null, { status: 204, statusText: 'No Content' });
    await harness.fixture.whenStable();

    expect(page().textContent).toContain('User reactivated.');
  });

  it('should save changed roles', async () => {
    await open();
    expect(button('Save roles')!.disabled).toBe(true);

    checkbox('Admin').click();
    await harness.fixture.whenStable();
    expect(button('Save roles')!.disabled).toBe(false);
    button('Save roles')!.click();

    const request = http.expectOne('/users/user-2/roles');
    expect([...request.request.body.roles].sort()).toEqual(['ADMIN', 'USER']);
    request.flush(userResponse({ userId: 'user-2', roles: ['ADMIN', 'USER'] }));
    await harness.fixture.whenStable();

    expect(page().textContent).toContain('Roles updated.');
    expect(button('Save roles')!.disabled).toBe(true);
  });

  it('should not allow saving with no roles', async () => {
    await open();
    checkbox('User').click();
    await harness.fixture.whenStable();

    expect(page().textContent).toContain('Choose at least one role.');
    expect(button('Save roles')!.disabled).toBe(true);
  });

  it('should show the server message when a change is rejected', async () => {
    await open();
    button('Suspend user')!.click();

    http
      .expectOne('/users/user-2/status')
      .flush(
        { detail: 'This account has been deleted and cannot be changed' },
        { status: 409, statusText: 'Conflict' },
      );
    await harness.fixture.whenStable();

    expect(page().querySelector('[role="alert"]')?.textContent).toContain(
      'This account has been deleted and cannot be changed',
    );
  });

  it('should hide the controls on your own account', async () => {
    await open(userResponse({ userId: 'admin-1', roles: ['ADMIN'] }));

    expect(page().textContent).toContain("You can't change your own status or roles.");
    expect(button('Suspend user')).toBeUndefined();
    expect(button('Save roles')).toBeUndefined();
  });

  it('should hide the controls on a deleted account', async () => {
    await open(
      userResponse({ userId: 'user-2', status: 'DELETED', firstName: 'Deleted', lastName: 'User' }),
    );

    expect(page().textContent).toContain("This account has been deleted and can't be changed.");
    expect(button('Reactivate user')).toBeUndefined();
    expect(button('Save roles')).toBeUndefined();
  });
});
