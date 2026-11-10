import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Keroma | Food delivery from restaurants near you',
    loadComponent: () => import('./pages/landing/landing').then((m) => m.Landing),
  },
  // Login, register etc. don't exist yet; send unknown URLs home instead of erroring.
  { path: '**', redirectTo: '' },
];
