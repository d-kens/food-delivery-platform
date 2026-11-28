import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { userResponse } from '../../testing/users';
import { GoogleReturn } from './google-return';

describe('GoogleReturn', () => {
  let googleReturn: GoogleReturn;

  const arriveAt = (path: string) => {
    vi.spyOn(TestBed.inject(Location), 'path').mockReturnValue(path);
  };
  const router = () => TestBed.inject(Router);

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    googleReturn = TestBed.inject(GoogleReturn);
  });

  it('should go to the remembered page once after a successful login', () => {
    const navigate = vi.spyOn(router(), 'navigateByUrl').mockResolvedValue(true);
    arriveAt('');
    googleReturn.remember('/account/security');

    googleReturn.resume(userResponse());
    googleReturn.resume(userResponse());

    expect(navigate).toHaveBeenCalledExactlyOnceWith('/account/security');
  });

  it('should not leave the site for an external address', () => {
    const navigate = vi.spyOn(router(), 'navigateByUrl').mockResolvedValue(true);
    arriveAt('');
    googleReturn.remember('//evil.example');

    googleReturn.resume(userResponse());

    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('should put ?next back on the login page after a failed login', () => {
    const navigate = vi.spyOn(router(), 'navigate').mockResolvedValue(true);
    arriveAt('/login?error=google');
    googleReturn.remember('/account');

    googleReturn.resume(null);

    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { error: 'google', next: '/account' },
      replaceUrl: true,
    });
  });

  it('should forget the page when the user ends up somewhere else', () => {
    const navigate = vi.spyOn(router(), 'navigateByUrl').mockResolvedValue(true);
    arriveAt('/partners');
    googleReturn.remember('/account');

    googleReturn.resume(userResponse());

    expect(navigate).not.toHaveBeenCalled();
    expect(sessionStorage.length).toBe(0);
  });

  it('should clear an earlier page when there is no ?next', () => {
    googleReturn.remember('/account');

    googleReturn.remember(null);

    expect(sessionStorage.length).toBe(0);
  });
});
