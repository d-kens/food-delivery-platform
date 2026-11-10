import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroBars3, heroXMark } from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-header',
  imports: [RouterLink, NgIcon],
  viewProviders: [provideIcons({ heroBars3, heroXMark })],
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
