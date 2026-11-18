import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth/auth.guards';
import { AuthLayout } from './layout/auth-layout/auth-layout';
import { MainLayout } from './layout/main-layout/main-layout';

export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    children: [
      {
        path: '',
        title: 'Keroma | Food delivery from restaurants near you',
        loadComponent: () => import('./pages/landing/landing').then((m) => m.Landing),
      },
      {
        path: 'account',
        canActivate: [authGuard],
        loadComponent: () => import('./pages/account/account').then((m) => m.Account),
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'profile' },
          {
            path: 'profile',
            title: 'Profile | Keroma',
            loadComponent: () => import('./pages/account/profile/profile').then((m) => m.Profile),
          },
          {
            path: 'security',
            title: 'Security | Keroma',
            loadComponent: () =>
              import('./pages/account/security/security').then((m) => m.Security),
          },
          { path: 'password', redirectTo: 'security' },
          { path: 'delete', redirectTo: 'security' },
        ],
      },
      {
        path: 'admin/users',
        canActivate: [adminGuard],
        title: 'Users | Keroma admin',
        loadComponent: () => import('./pages/admin/user-list/user-list').then((m) => m.UserList),
      },
      {
        path: 'admin/users/:id',
        canActivate: [adminGuard],
        title: 'User | Keroma admin',
        loadComponent: () =>
          import('./pages/admin/user-detail/user-detail').then((m) => m.UserDetail),
      },
    ],
  },
  {
    path: '',
    component: AuthLayout,
    children: [
      {
        path: 'login',
        title: 'Log in | Keroma',
        loadComponent: () => import('./pages/auth/login/login').then((m) => m.Login),
      },
      {
        path: 'register',
        title: 'Create an account | Keroma',
        loadComponent: () => import('./pages/auth/register/register').then((m) => m.Register),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
