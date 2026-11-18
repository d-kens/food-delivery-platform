import { Component, inject } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroXMark } from '@ng-icons/heroicons/outline';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toasts',
  imports: [NgIcon],
  viewProviders: [provideIcons({ heroXMark })],
  template: `
    <div
      class="pointer-events-none fixed inset-x-4 bottom-4 z-50 flex flex-col gap-2 sm:right-4 sm:left-auto sm:w-96"
      aria-live="polite"
    >
      @for (toast of toasts.toasts(); track toast.id) {
        <div
          class="pointer-events-auto flex items-start gap-3 rounded-lg px-4 py-3 text-sm shadow-lg ring-1"
          [class]="
            toast.tone === 'success'
              ? 'bg-emerald-50 text-emerald-800 ring-emerald-200'
              : 'bg-red-50 text-red-800 ring-red-200'
          "
          [attr.role]="toast.tone === 'success' ? 'status' : 'alert'"
        >
          <p class="flex-1">{{ toast.text }}</p>
          <button
            type="button"
            class="-m-1 grid size-7 shrink-0 place-items-center rounded-md opacity-70 hover:opacity-100"
            aria-label="Dismiss"
            (click)="toasts.dismiss(toast.id)"
          >
            <ng-icon name="heroXMark" size="16" />
          </button>
        </div>
      }
    </div>
  `,
})
export class Toasts {
  protected readonly toasts = inject(ToastService);
}
