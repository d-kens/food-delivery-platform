import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Role, UserResponse, UserStatus } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { genericErrorMessage, problemOf, statusOf } from '../../../core/http/problem-detail';
import { UsersService } from '../../../core/users/users.service';
import { Spinner } from '../../../shared/spinner/spinner';
import { StatusBadge } from '../../../shared/status-badge/status-badge';
import { ToastService } from '../../../shared/toast/toast.service';

const ROLES: { value: Role; label: string; description: string }[] = [
  { value: 'USER', label: 'User', description: 'Can order food' },
  { value: 'RIDER', label: 'Rider', description: 'Can deliver orders' },
  { value: 'ADMIN', label: 'Admin', description: 'Can manage users' },
];

@Component({
  selector: 'app-user-detail',
  imports: [RouterLink, Spinner, StatusBadge],
  templateUrl: './user-detail.html',
})
export class UserDetail {
  private readonly users = inject(UsersService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly id = input.required<string>();

  protected readonly roleOptions = ROLES;
  protected readonly user = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.users.getUser(params),
  });

  protected readonly selectedRoles = signal<Set<Role> | null>(null);
  protected readonly busy = signal<'status' | 'roles' | null>(null);

  protected readonly isSelf = computed(() => this.user.value()?.userId === this.auth.user()?.id);
  protected readonly isDeleted = computed(() => this.user.value()?.status === 'DELETED');
  protected readonly canEdit = computed(() => !this.isSelf() && !this.isDeleted());
  protected readonly notFound = computed(() => statusOf(this.user.error()) === 404);

  protected readonly roles = computed(
    () => this.selectedRoles() ?? new Set((this.user.value()?.roles ?? []) as Role[]),
  );
  protected readonly rolesChanged = computed(() => {
    const current = new Set(this.user.value()?.roles ?? []);
    const selected = this.roles();
    return selected.size !== current.size || [...selected].some((role) => !current.has(role));
  });

  protected toggleRole(role: Role, checked: boolean): void {
    const next = new Set(this.roles());
    if (checked) {
      next.add(role);
    } else {
      next.delete(role);
    }
    this.selectedRoles.set(next);
  }

  protected saveRoles(): void {
    const user = this.user.value();
    if (!user || this.busy() || this.roles().size === 0) {
      return;
    }
    this.busy.set('roles');
    this.users
      .updateRoles(user.userId, [...this.roles()])
      .pipe(finalize(() => this.busy.set(null)))
      .subscribe({
        next: (updated) => {
          this.user.set(updated);
          this.selectedRoles.set(null);
          this.toast.success(
            'Roles updated. They apply the next time this user refreshes their session.',
          );
        },
        error: (error) => this.toast.error(this.errorText(error)),
      });
  }

  protected setStatus(status: UserStatus): void {
    const user = this.user.value();
    if (!user || this.busy()) {
      return;
    }
    this.busy.set('status');
    this.users
      .updateStatus(user.userId, status)
      .pipe(finalize(() => this.busy.set(null)))
      .subscribe({
        next: () => {
          this.user.set({ ...user, status } satisfies UserResponse);
          this.toast.success(
            status === 'SUSPENDED'
              ? 'User suspended and logged out of every device.'
              : 'User reactivated.',
          );
        },
        error: (error) => this.toast.error(this.errorText(error)),
      });
  }

  private errorText(error: unknown): string {
    const status = statusOf(error);
    if (status === 400 || status === 404 || status === 409) {
      return problemOf(error).detail ?? 'This change could not be made.';
    }
    return genericErrorMessage(error);
  }
}
