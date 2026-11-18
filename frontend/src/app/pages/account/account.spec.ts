import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthService } from '../../core/auth/auth.service';
import { userResponse } from '../../testing/users';
import { Account } from './account';

describe('Account', () => {
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'account',
            component: Account,
            children: [{ path: 'profile', children: [] }],
          },
        ]),
      ],
    });
    TestBed.inject(AuthService).updateSessionUser(userResponse());
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/account/profile');
  });

  const page = (): HTMLElement => harness.fixture.nativeElement as HTMLElement;

  it('should show who is signed in', () => {
    expect(page().textContent).toContain('JD');
    expect(page().textContent).toContain('Jane Doe');
    expect(page().textContent).toContain('jane.doe@gmail.com');
  });

  it('should link to each section and mark the current one', () => {
    const links = [...page().querySelectorAll<HTMLAnchorElement>('nav a')];

    expect(links.map((link) => link.textContent?.trim())).toEqual(['Profile', 'Security']);
    expect(links[0].getAttribute('aria-current')).toBe('page');
    expect(links[1].getAttribute('aria-current')).toBeNull();
  });
});
