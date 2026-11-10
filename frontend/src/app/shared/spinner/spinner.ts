import { Component } from '@angular/core';

@Component({
  selector: 'app-spinner',
  host: { class: 'inline-block size-4 shrink-0', role: 'status', 'aria-label': 'Loading' },
  template: `<span class="block size-full animate-spin rounded-full border-2 border-current border-r-transparent"></span>`,
})
export class Spinner {}
