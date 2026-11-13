import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { Role, UserResponse, UserStatus } from '../../../core/auth/auth.models';
import { Page, UserQuery } from '../../../core/users/users.models';
import { UsersService } from '../../../core/users/users.service';
import { Spinner } from '../../../shared/spinner/spinner';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

const PAGE_SIZE = 20;
const STATUSES: UserStatus[] = ['ACTIVE', 'SUSPENDED', 'DELETED'];
const ROLES: Role[] = ['USER', 'RIDER', 'ADMIN'];

type ListState =
  | { kind: 'loading'; query: UserQuery }
  | { kind: 'loaded'; query: UserQuery; page: Page<UserResponse> }
  | { kind: 'error'; query: UserQuery };

function toQuery(params: ParamMap): UserQuery {
  const status = params.get('status') as UserStatus | null;
  const role = params.get('role') as Role | null;
  const page = Number(params.get('page'));
  return {
    status: status && STATUSES.includes(status) ? status : undefined,
    role: role && ROLES.includes(role) ? role : undefined,
    search: params.get('search')?.trim() || undefined,
    page: Number.isInteger(page) && page > 0 ? page : 0,
    size: PAGE_SIZE,
  };
}

@Component({
  selector: 'app-user-list',
  imports: [RouterLink, Spinner, StatusBadge],
  templateUrl: './user-list.html',
})
export class UserList {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly users = inject(UsersService);

  private readonly reload = new BehaviorSubject<void>(undefined);

  protected readonly statuses = STATUSES;
  protected readonly roles = ROLES;

  protected readonly state = toSignal(
    combineLatest([this.route.queryParamMap, this.reload]).pipe(
      map(([params]) => toQuery(params)),
      switchMap((query) =>
        this.users.listUsers(query).pipe(
          map((page): ListState => ({ kind: 'loaded', query, page })),
          catchError(() => of<ListState>({ kind: 'error', query })),
          startWith<ListState>({ kind: 'loading', query }),
        ),
      ),
    ),
    { initialValue: { kind: 'loading', query: {} } as ListState },
  );

  protected label(value: string): string {
    return value.charAt(0) + value.slice(1).toLowerCase();
  }

  protected onSearch(event: Event, search: string): void {
    event.preventDefault();
    this.navigate({ search: search.trim() || null, page: null });
  }

  protected onFilter(name: 'status' | 'role', value: string): void {
    this.navigate({ [name]: value || null, page: null });
  }

  protected goToPage(page: number): void {
    this.navigate({ page: page > 0 ? page : null });
  }

  protected retry(): void {
    this.reload.next();
  }

  private navigate(queryParams: Record<string, string | number | null>): void {
    this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge' });
  }
}
