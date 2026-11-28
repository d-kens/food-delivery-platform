import { Location } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { UserResponse } from './auth.models';
import { safeRedirect } from './auth.service';

const KEY = 'google-login-next';

@Injectable({ providedIn: 'root' })
export class GoogleReturn {
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  remember(next: string | null): void {
    if (next) {
      sessionStorage.setItem(KEY, next);
    } else {
      sessionStorage.removeItem(KEY);
    }
  }

  resume(user: UserResponse | null): void {
    const next = sessionStorage.getItem(KEY);
    sessionStorage.removeItem(KEY);
    if (!next) {
      return;
    }

    const path = this.location.path();
    if (user && path === '') {
      void this.router.navigateByUrl(safeRedirect(next));
    } else if (!user && path.startsWith('/login?error=google')) {
      void this.router.navigate(['/login'], {
        queryParams: { error: 'google', next },
        replaceUrl: true,
      });
    }
  }
}
