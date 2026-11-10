import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Footer } from '../footer/footer';
import { Header } from '../header/header';

/** Public marketing pages: full header and footer. */
@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, Header, Footer],
  template: `
    <app-header />
    <main>
      <router-outlet />
    </main>
    <app-footer />
  `,
})
export class MainLayout {}
