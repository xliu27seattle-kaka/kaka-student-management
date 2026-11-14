import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from '../auth/auth-store';

/** Codes that mean "this session is over", as opposed to "wrong password". */
const SESSION_ENDED = new Set(['UNAUTHENTICATED', 'SESSION_REVOKED']);

/**
 * When the backend says the session is over (token expired, account deactivated), signs the user
 * out and returns to the login page. Every other error is passed on for the page to show.
 */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const store = inject(AuthStore);
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      const code = error instanceof HttpErrorResponse ? error.error?.code : undefined;
      if (error instanceof HttpErrorResponse && error.status === 401 && SESSION_ENDED.has(code)) {
        store.logout();
        void router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
