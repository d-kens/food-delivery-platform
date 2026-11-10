import { Component, computed, input } from '@angular/core';

// Heroicons (MIT) outline paths, 24x24 viewBox.
const ICONS = {
  'arrow-right': ['M13.5 4.5 21 12m0 0-7.5 7.5M21 12H3'],
  bars: ['M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5'],
  'map-pin': [
    'M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
    'M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z',
  ],
  star: [
    'M11.48 3.499a.562.562 0 0 1 1.04 0l2.125 5.111a.563.563 0 0 0 .475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 0 0-.182.557l1.285 5.385a.562.562 0 0 1-.84.61l-4.725-2.885a.562.562 0 0 0-.586 0L6.982 20.54a.562.562 0 0 1-.84-.61l1.285-5.386a.562.562 0 0 0-.182-.557l-4.204-3.602a.562.562 0 0 1 .321-.988l5.518-.442a.563.563 0 0 0 .475-.345L11.48 3.5Z',
  ],
  'x-mark': ['M6 18 18 6M6 6l12 12'],
} as const;

export type IconName = keyof typeof ICONS;

@Component({
  selector: 'app-icon',
  host: { class: 'inline-block shrink-0' },
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      stroke="currentColor"
      stroke-width="1.75"
      [attr.fill]="filled() ? 'currentColor' : 'none'"
      class="size-full"
      aria-hidden="true"
    >
      @for (d of paths(); track $index) {
        <path stroke-linecap="round" stroke-linejoin="round" [attr.d]="d" />
      }
    </svg>
  `,
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly filled = input(false);

  protected readonly paths = computed(() => ICONS[this.name()]);
}
