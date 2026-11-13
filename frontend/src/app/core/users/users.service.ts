import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Role, UserResponse, UserStatus } from '../auth/auth.models';
import { AuthService } from '../auth/auth.service';
import { Page, UpdateProfileRequest, UserQuery } from './users.models';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly usersUrl = `${environment.apiUrl}/users`;

  updateProfile(request: UpdateProfileRequest): Observable<UserResponse> {
    return this.http
      .patch<UserResponse>(`${this.usersUrl}/me`, request)
      .pipe(tap((response) => this.auth.updateSessionUser(response)));
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http
      .patch<void>(`${this.usersUrl}/me/password`, { currentPassword, newPassword })
      .pipe(
        tap(() => this.auth.clearSession()),
        map(() => undefined),
      );
  }

  deleteAccount(currentPassword: string): Observable<void> {
    return this.http.delete<void>(`${this.usersUrl}/me`, { body: { currentPassword } }).pipe(
      tap(() => this.auth.clearSession()),
      map(() => undefined),
    );
  }

  listUsers(query: UserQuery): Observable<Page<UserResponse>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<UserResponse>>(this.usersUrl, { params });
  }

  getUser(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.usersUrl}/${encodeURIComponent(id)}`);
  }

  updateStatus(id: string, status: UserStatus): Observable<void> {
    return this.http.patch<void>(`${this.usersUrl}/${encodeURIComponent(id)}/status`, { status });
  }

  updateRoles(id: string, roles: Role[]): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.usersUrl}/${encodeURIComponent(id)}/roles`, {
      roles,
    });
  }
}
