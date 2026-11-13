import { Component, computed, input } from '@angular/core';
import { UserStatus } from '../../core/auth/auth.models';

@Component({
  selector: 'app-status-badge',
  host: { class: 'inline-flex' },
  template: `
    <span
      class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1"
      [class]="tone()"
    >
      {{ label() }}
    </span>
  `,
})
export class StatusBadge {
  readonly status = input.required<UserStatus>();

  protected readonly label = computed(() => {
    const status = this.status();
    return status.charAt(0) + status.slice(1).toLowerCase();
  });

  protected readonly tone = computed(() => {
    switch (this.status()) {
      case 'ACTIVE':
        return 'bg-emerald-50 text-emerald-800 ring-emerald-200';
      case 'SUSPENDED':
        return 'bg-amber-50 text-amber-800 ring-amber-200';
      default:
        return 'bg-zinc-100 text-zinc-600 ring-zinc-200';
    }
  });
}
