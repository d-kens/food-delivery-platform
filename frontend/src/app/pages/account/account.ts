import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-account',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 class="font-display text-3xl font-bold tracking-tight">Account settings</h1>

      <nav
        class="mt-6 flex gap-1 overflow-x-auto border-b border-zinc-200"
        aria-label="Account sections"
      >
        @for (tab of tabs; track tab.path) {
          <a
            [routerLink]="tab.path"
            routerLinkActive="border-zinc-900 text-zinc-900"
            [routerLinkActiveOptions]="{ exact: true }"
            ariaCurrentWhenActive="page"
            class="-mb-px shrink-0 border-b-2 border-transparent px-3 py-2.5 text-sm font-medium text-zinc-500 transition-colors hover:text-zinc-900"
          >
            {{ tab.label }}
          </a>
        }
      </nav>

      <div class="mt-8">
        <router-outlet />
      </div>
    </div>
  `,
})
export class Account {
  protected readonly tabs = [
    { label: 'Profile', path: '/account/profile' },
    { label: 'Password', path: '/account/password' },
    { label: 'Delete account', path: '/account/delete' },
  ];
}
