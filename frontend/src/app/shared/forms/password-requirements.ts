import { Component, computed, input } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroCheckCircleSolid } from '@ng-icons/heroicons/solid';

export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;

@Component({
  selector: 'app-password-requirements',
  imports: [NgIcon],
  viewProviders: [provideIcons({ heroCheckCircleSolid })],
  host: { class: 'block' },
  template: `
    <ul
      class="flex flex-wrap gap-x-5 gap-y-1.5 text-xs font-medium"
      aria-label="Password requirements"
    >
      @for (rule of rules(); track rule.label) {
        <li
          class="flex items-center gap-1.5 transition-colors"
          [class]="rule.met ? 'text-emerald-700' : 'text-zinc-500'"
        >
          @if (rule.met) {
            <ng-icon
              name="heroCheckCircleSolid"
              size="16"
              class="text-emerald-600"
              aria-hidden="true"
            />
          } @else {
            <span class="size-3.5 rounded-full border-2 border-zinc-300" aria-hidden="true"></span>
          }
          {{ rule.label }}
          <span class="sr-only">{{ rule.met ? '(met)' : '(not met)' }}</span>
        </li>
      }
    </ul>
  `,
})
export class PasswordRequirements {
  readonly password = input('');
  readonly confirmation = input('');

  protected readonly rules = computed(() => [
    {
      label: `At least ${PASSWORD_MIN_LENGTH} characters`,
      met: this.password().length >= PASSWORD_MIN_LENGTH,
    },
    {
      label: 'Passwords match',
      met: this.confirmation().length > 0 && this.confirmation() === this.password(),
    },
  ]);
}
