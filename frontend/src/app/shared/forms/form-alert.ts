import { Component, ElementRef, afterRenderEffect, inject, input } from '@angular/core';

@Component({
  selector: 'app-form-alert',
  host: { class: 'block' },
  template: `
    @if (message()) {
      <div
        class="mb-3 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-800 ring-1 ring-red-200"
        role="alert"
      >
        <p>{{ message() }}</p>
        <ng-content />
      </div>
    }
  `,
})
export class FormAlert {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  readonly message = input<string | null>(null);

  constructor() {
    afterRenderEffect(() => {
      if (this.message()) {
        this.host.scrollIntoView?.({ block: 'nearest', behavior: 'smooth' });
      }
    });
  }
}
