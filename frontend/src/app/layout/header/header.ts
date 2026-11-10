import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-header',
  imports: [RouterLink, Icon],
  templateUrl: './header.html',
})
export class Header {
  protected readonly menuOpen = signal(false);

  protected readonly links = [
    { label: 'Restaurants', fragment: 'restaurants' },
    { label: 'How it works', fragment: 'how-it-works' },
    { label: 'Partners', fragment: 'partners' },
  ];

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }
}
