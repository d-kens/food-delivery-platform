import { Component, computed, input } from '@angular/core';

/**
 * The line under a form field. Always rendered with a fixed minimum height so
 * showing or clearing an error never shifts the layout. Shows, in priority
 * order: an error, a warning (e.g. Caps Lock), or a hint.
 */
@Component({
  selector: 'app-field-message',
  host: { class: 'block' },
  template: `
    <p
      [id]="fieldId() + '-message'"
      class="mt-1 min-h-4 text-xs leading-4"
      [class]="toneClass()"
      [attr.data-state]="state()"
    >
      @if (error()) {
        {{ error() }}
      } @else if (warning()) {
        {{ warning() }}
      } @else {
        {{ hint() }}
      }
    </p>
  `,
})
export class FieldMessage {
  readonly fieldId = input.required<string>();
  readonly error = input<string | null>(null);
  readonly warning = input<string | null>(null);
  readonly hint = input<string | null>(null);

  protected readonly state = computed(() => (this.error() ? 'error' : this.warning() ? 'warning' : 'hint'));

  protected readonly toneClass = computed(() => {
    switch (this.state()) {
      case 'error':
        return 'text-red-600';
      case 'warning':
        return 'text-amber-700';
      default:
        return 'text-zinc-500';
    }
  });
}
