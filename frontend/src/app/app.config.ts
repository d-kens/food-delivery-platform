import { IMAGE_LOADER, ImageLoaderConfig } from '@angular/common';
import { provideHttpClient, withFetch } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideNgIconsConfig } from '@ng-icons/core';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';

const unsplashLoader = ({ src, width }: ImageLoaderConfig) =>
  `https://images.unsplash.com/${src}?auto=format&fit=crop&q=75${width ? `&w=${width}` : ''}`;

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }),
    ),
    provideHttpClient(withFetch()),
    provideAppInitializer(() => {
      inject(AuthService).restoreSession().subscribe();
    }),
    { provide: IMAGE_LOADER, useValue: unsplashLoader },
    provideNgIconsConfig({ strokeWidth: 1.75 }),
  ],
};
