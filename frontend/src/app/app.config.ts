import { IMAGE_LOADER, ImageLoaderConfig } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideNgIconsConfig } from '@ng-icons/core';
import { routes } from './app.routes';
import { authRefreshInterceptor } from './core/auth/auth-refresh.interceptor';
import { AuthService } from './core/auth/auth.service';

const unsplashLoader = ({ src, width }: ImageLoaderConfig) =>
  `https://images.unsplash.com/${src}?auto=format&fit=crop&q=75${width ? `&w=${width}` : ''}`;

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }),
      withComponentInputBinding(),
    ),
    provideHttpClient(withFetch(), withInterceptors([authRefreshInterceptor])),
    provideAppInitializer(() => {
      inject(AuthService).restoreSession().subscribe();
    }),
    { provide: IMAGE_LOADER, useValue: unsplashLoader },
    provideNgIconsConfig({ strokeWidth: 1.75 }),
  ],
};
