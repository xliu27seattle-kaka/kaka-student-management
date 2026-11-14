import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../auth/auth-store';
import { HOME_BY_ROLE, Role } from '../auth/models';

/**
 * Guards an area that belongs to one role. A visitor goes to the login page; a signed-in user
 * with another role goes to their own home instead of seeing an error.
 */
export function roleGuard(role: Role): CanActivateFn {
  return () => {
    const session = inject(AuthStore).snapshot;
    const router = inject(Router);
    if (!session) return router.parseUrl('/login');
    if (session.user.role !== role) return router.parseUrl(HOME_BY_ROLE[session.user.role]);
    return true;
  };
}

/** For the login and registration pages: a signed-in user is sent straight to their home. */
export const guestGuard: CanActivateFn = () => {
  const session = inject(AuthStore).snapshot;
  return session ? inject(Router).parseUrl(HOME_BY_ROLE[session.user.role]) : true;
};
