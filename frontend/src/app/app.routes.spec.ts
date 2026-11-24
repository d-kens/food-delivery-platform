import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, provideRouter } from '@angular/router';
import { routes } from './app.routes';

describe('routes', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  const deepestPath = (): string | undefined => {
    let route: ActivatedRouteSnapshot = TestBed.inject(Router).routerState.snapshot.root;
    while (route.firstChild) {
      route = route.firstChild;
    }
    return route.routeConfig?.path;
  };

  it.each(['account/delete/confirm', 'account/email/confirm'])(
    'should open /%s without logging in',
    async (path) => {
      await TestBed.inject(Router).navigateByUrl(`/${path}?token=raw-token`);

      expect(deepestPath()).toBe(path);
    },
  );
});
