import { IMAGE_LOADER, ImageLoaderConfig } from '@angular/common';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';

// Placeholder photos are served from Unsplash; ngSrc takes the photo id, the loader adds sizing.
const unsplashLoader = ({ src, width }: ImageLoaderConfig) =>
  `https://images.unsplash.com/${src}?auto=format&fit=crop&q=75${width ? `&w=${width}` : ''}`;

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }),
    ),
    { provide: IMAGE_LOADER, useValue: unsplashLoader },
  ],
};
