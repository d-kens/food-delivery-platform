import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const RETRIED_AFTER_REFRESH = new HttpContextToken<boolean>(() => false);

export const authRefreshInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);

  if (auth.isAuthEndpoint(request.url) || request.context.get(RETRIED_AFTER_REFRESH)) {
    return next(request);
  }

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        catchError(() => {
          auth.clearSession();
          return throwError(() => error);
        }),
        switchMap(() => next(request.clone({ context: request.context.set(RETRIED_AFTER_REFRESH, true) }))),
      );
    }),
  );
};
