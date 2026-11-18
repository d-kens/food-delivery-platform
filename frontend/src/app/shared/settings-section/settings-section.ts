import { Component, input } from '@angular/core';

@Component({
  selector: 'app-settings-section',
  host: { class: 'grid gap-x-10 gap-y-5 md:grid-cols-3' },
  template: `
    <div>
      <h2 [id]="headingId()" class="text-base font-semibold text-zinc-900">{{ heading() }}</h2>
      <p class="mt-1 text-sm leading-6 text-zinc-600">{{ description() }}</p>
    </div>
    <div class="md:col-span-2" role="group" [attr.aria-labelledby]="headingId()">
      <ng-content />
    </div>
  `,
})
export class SettingsSection {
  readonly heading = input.required<string>();
  readonly description = input.required<string>();
  readonly headingId = input.required<string>();
}
