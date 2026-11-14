import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthStore } from './core/auth/auth-store';
import { HOME_BY_ROLE } from './core/auth/models';
import { guestGuard, roleGuard } from './core/guards/role-guard';

/**
 * One guarded area per role. Each page is loaded only when it is first visited.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => {
      const session = inject(AuthStore).snapshot;
      return session ? HOME_BY_ROLE[session.user.role] : '/login';
    },
  },
  {
    path: 'login',
    title: 'Sign in · Kaka Student Management',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    title: 'Create an account · Kaka Student Management',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'admin',
    title: 'Administration · Kaka Student Management',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () => import('./features/home/role-home-page').then((m) => m.RoleHomePage),
  },
  {
    path: 'professor',
    title: 'Professor · Kaka Student Management',
    canActivate: [roleGuard('PROFESSOR')],
    loadComponent: () => import('./features/home/role-home-page').then((m) => m.RoleHomePage),
  },
  {
    path: 'student',
    title: 'Student · Kaka Student Management',
    canActivate: [roleGuard('STUDENT')],
    loadComponent: () => import('./features/home/role-home-page').then((m) => m.RoleHomePage),
  },
  {
    path: '**',
    title: 'Page not found · Kaka Student Management',
    loadComponent: () => import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
  },
];
