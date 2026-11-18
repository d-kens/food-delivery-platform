import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-account',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
      <div class="flex items-center gap-4 pt-10 pb-8">
        <span
          class="grid size-14 shrink-0 place-items-center rounded-full bg-brand-500 font-display text-lg font-bold text-white"
          aria-hidden="true"
        >
          {{ initials() }}
        </span>
        <div class="min-w-0">
          <h1 class="font-display text-2xl font-bold tracking-tight sm:text-3xl">
            Account settings
          </h1>
          @if (user(); as current) {
            <p class="mt-0.5 truncate text-sm text-zinc-600">
              {{ current.firstName }} {{ current.lastName }} · {{ current.email }}
            </p>
          }
        </div>
      </div>

      <nav class="flex gap-8 border-b border-zinc-200" aria-label="Account sections">
        @for (section of sections; track section.path) {
          <a
            [routerLink]="section.path"
            routerLinkActive="border-brand-500 text-zinc-900"
            ariaCurrentWhenActive="page"
            class="-mb-px border-b-2 border-transparent pb-3 text-sm font-semibold text-zinc-500 transition-colors hover:text-zinc-900"
          >
            {{ section.label }}
          </a>
        }
      </nav>

      <div class="py-10">
        <router-outlet />
      </div>
    </div>
  `,
})
export class Account {
  private readonly auth = inject(AuthService);

  protected readonly user = this.auth.user;
  protected readonly initials = computed(() => {
    const user = this.user();
    return user ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase() : '';
  });

  protected readonly sections = [
    { label: 'Profile', path: '/account/profile' },
    { label: 'Security', path: '/account/security' },
  ];
}
