import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { userResponse } from '../../../testing/users';
import { Security } from './security';

describe('Security', () => {
  it('should show the password card and the delete account card', async () => {
    TestBed.configureTestingModule({
      imports: [Security],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthService).updateSessionUser(userResponse());
    const fixture = TestBed.createComponent(Security);
    await fixture.whenStable();
    const headings = [...(fixture.nativeElement as HTMLElement).querySelectorAll('h2')];

    expect(headings.map((heading) => heading.textContent?.trim())).toEqual([
      'Change password',
      'Delete account',
    ]);
  });
});
