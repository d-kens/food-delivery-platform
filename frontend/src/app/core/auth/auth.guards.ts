import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth
    .whenRestored()
    .pipe(
      map(
        () =>
          auth.isLoggedIn() ||
          router.createUrlTree(['/login'], { queryParams: { next: state.url } }),
      ),
    );
};

export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.whenRestored().pipe(
    map(() => {
      if (!auth.isLoggedIn()) {
        return router.createUrlTree(['/login'], { queryParams: { next: state.url } });
      }
      return auth.isAdmin() || router.createUrlTree(['/']);
    }),
  );
};
