import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  heroArrowRight,
  heroBuildingStorefront,
  heroChevronRight,
  heroLifebuoy,
  heroMapPin,
  heroQuestionMarkCircle,
  heroTruck,
} from '@ng-icons/heroicons/outline';
import { filter } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';

const DESKTOP_BREAKPOINT = 768;

@Component({
  selector: 'app-header',
  imports: [RouterLink, NgIcon],
  viewProviders: [
    provideIcons({
      heroArrowRight,
      heroBuildingStorefront,
      heroChevronRight,
      heroLifebuoy,
      heroMapPin,
      heroQuestionMarkCircle,
      heroTruck,
    }),
  ],
  host: {
    '(document:keydown)': 'onKeydown($event)',
    '(window:resize)': 'onResize()',
  },
  templateUrl: './header.html',
})
export class Header {
  private readonly document = inject(DOCUMENT);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly menuToggle = viewChild.required<ElementRef<HTMLButtonElement>>('menuToggle');
  private readonly mobileMenu = viewChild<ElementRef<HTMLElement>>('mobileMenu');

  protected readonly menuOpen = signal(false);
  protected readonly user = this.auth.user;
  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly initials = computed(() => {
    const user = this.user();
    return user ? `${user.firstName[0] ?? ''}${user.lastName[0] ?? ''}`.toUpperCase() : '';
  });

  protected readonly links = [
    { label: 'Restaurants', fragment: 'restaurants' },
    { label: 'How it works', fragment: 'how-it-works' },
    { label: 'Partners', fragment: 'partners' },
  ];

  protected readonly mobileLinks = [
    { label: 'Restaurants', description: 'Popular places near you', fragment: 'restaurants', icon: 'heroBuildingStorefront' },
    { label: 'How it works', description: 'Order in three simple steps', fragment: 'how-it-works', icon: 'heroQuestionMarkCircle' },
  ];

  protected readonly partnerLinks = [
    {
      label: 'Add your restaurant',
      description: 'Reach more customers',
      path: '/partners/restaurants',
      icon: 'heroBuildingStorefront',
    },
    {
      label: 'Become a rider',
      description: 'Earn on your schedule',
      path: '/partners/riders',
      icon: 'heroTruck',
    },
  ];

  constructor() {
    effect(() => {
      this.document.body.style.overflow = this.menuOpen() ? 'hidden' : '';
    });
    effect(() => this.mobileMenu()?.nativeElement.focus());
    inject(DestroyRef).onDestroy(() => (this.document.body.style.overflow = ''));

    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.closeMenu());
  }

  protected toggleMenu(): void {
    if (this.menuOpen()) {
      this.closeMenu(true);
    } else {
      this.menuOpen.set(true);
    }
  }

  protected closeMenu(restoreFocus = false): void {
    if (!this.menuOpen()) {
      return;
    }
    this.menuOpen.set(false);
    if (restoreFocus) {
      this.menuToggle().nativeElement.focus();
    }
  }

  protected logout(): void {
    this.closeMenu();
    this.auth.logout().subscribe(() => this.router.navigateByUrl('/'));
  }

  protected onSearch(event: Event): void {
    event.preventDefault();
    this.closeMenu();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (!this.menuOpen()) {
      return;
    }
    if (event.key === 'Escape') {
      this.closeMenu(true);
      return;
    }
    if (event.key === 'Tab') {
      this.keepFocusInMenu(event);
    }
  }

  protected onResize(): void {
    if (this.document.defaultView!.innerWidth >= DESKTOP_BREAKPOINT) {
      this.closeMenu();
    }
  }

  private keepFocusInMenu(event: KeyboardEvent): void {
    const panel = this.mobileMenu()?.nativeElement;
    if (!panel) {
      return;
    }
    const focusable = [
      this.menuToggle().nativeElement,
      ...panel.querySelectorAll<HTMLElement>('a[href], button:not([disabled]), input:not([disabled])'),
    ];
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = this.document.activeElement;

    if (event.shiftKey && (active === first || active === panel)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }
}
