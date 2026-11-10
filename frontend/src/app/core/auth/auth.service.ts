import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, SessionUser } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authUrl = `${environment.apiUrl}/auth`;
  private readonly usersUrl = `${environment.apiUrl}/users`;

  private readonly currentUser = signal<SessionUser | null>(null);

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);

  login(request: LoginRequest): Observable<SessionUser> {
    return this.http
      .post<AuthResponse>(`${this.authUrl}/login`, request)
      .pipe(map((response) => this.startSession(response)));
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<unknown>(`${this.authUrl}/register`, request).pipe(map(() => undefined));
  }

  restoreSession(): Observable<SessionUser | null> {
    return this.http.get<AuthResponse>(`${this.usersUrl}/me`).pipe(
      catchError((error: unknown) =>
        error instanceof HttpErrorResponse && error.status === 401
          ? this.http.post<AuthResponse>(`${this.authUrl}/refresh-token`, null)
          : throwError(() => error),
      ),
      map((response) => this.startSession(response)),
      catchError(() => {
        this.currentUser.set(null);
        return of(null);
      }),
    );
  }

  clearSession(): void {
    this.currentUser.set(null);
  }

  private startSession(response: AuthResponse): SessionUser {
    const user: SessionUser = {
      id: response.userId,
      firstName: response.firstName,
      lastName: response.lastName,
      email: response.email,
      roles: response.roles,
    };
    this.currentUser.set(user);
    return user;
  }
}

export function safeRedirect(next: string | null | undefined): string {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : '/';
}
