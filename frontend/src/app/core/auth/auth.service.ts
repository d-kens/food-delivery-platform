import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, ReplaySubject, catchError, finalize, map, of, shareReplay, take } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, RegisterRequest, UserResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authUrl = `${environment.apiUrl}/user-service/auth`;
  private readonly usersUrl = `${environment.apiUrl}/user-service/users`;

  private readonly currentUser = signal<UserResponse | null>(null);
  private refreshInFlight: Observable<UserResponse> | null = null;
  private readonly restored = new ReplaySubject<void>(1);

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);
  readonly isAdmin = computed(() => this.currentUser()?.roles.includes('ADMIN') ?? false);

  login(request: LoginRequest): Observable<UserResponse> {
    return this.http
      .post<UserResponse>(`${this.authUrl}/login`, request)
      .pipe(map((response) => this.startSession(response)));
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<unknown>(`${this.authUrl}/register`, request).pipe(map(() => undefined));
  }

  forgotPassword(email: string): Observable<void> {
    return this.http
      .post<unknown>(`${this.authUrl}/forgot-password`, { email })
      .pipe(map(() => undefined));
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http
      .post<unknown>(`${this.authUrl}/reset-password`, { token, newPassword })
      .pipe(map(() => undefined));
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.authUrl}/logout`, null).pipe(
      catchError(() => of(undefined)),
      map(() => undefined),
      finalize(() => this.clearSession()),
    );
  }

  restoreSession(): Observable<UserResponse | null> {
    return this.http.get<UserResponse>(`${this.usersUrl}/me`).pipe(
      map((response) => this.startSession(response)),
      catchError(() => {
        this.clearSession();
        return of(null);
      }),
      finalize(() => this.restored.next()),
    );
  }

  whenRestored(): Observable<void> {
    return this.restored.pipe(take(1));
  }

  updateSessionUser(response: UserResponse): UserResponse {
    return this.startSession(response);
  }

  refresh(): Observable<UserResponse> {
    this.refreshInFlight ??= this.http
      .post<UserResponse>(`${this.authUrl}/refresh-token`, null)
      .pipe(
        map((response) => this.startSession(response)),
        finalize(() => (this.refreshInFlight = null)),
        shareReplay(1),
      );
    return this.refreshInFlight;
  }

  clearSession(): void {
    this.currentUser.set(null);
  }

  isAuthEndpoint(url: string): boolean {
    return url.startsWith(`${this.authUrl}/`);
  }

  private startSession(response: UserResponse): UserResponse {
    this.currentUser.set(response);
    return response;
  }
}

export function safeRedirect(next: string | null | undefined): string {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : '/';
}
