import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { AuthStore } from '../auth/auth-store';
import { Role, Session } from '../auth/models';
import { guestGuard, roleGuard } from './role-guard';

function sessionOf(role: Role): Session {
  return {
    accessToken: 'jwt-value',
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    user: { id: 1, email: 'someone@uni.example', firstName: 'Some', lastName: 'One', role },
  };
}

function run(guard: ReturnType<typeof roleGuard>, snapshot: Session | null) {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthStore, useValue: { snapshot } }],
  });
  const result = TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  );
  return result instanceof UrlTree ? TestBed.inject(Router).serializeUrl(result) : result;
}

describe('roleGuard', () => {
  it('sends a visitor who is not signed in to the login page', () => {
    expect(run(roleGuard('ADMIN'), null)).toBe('/login');
  });

  it('lets the matching role in', () => {
    expect(run(roleGuard('ADMIN'), sessionOf('ADMIN'))).toBe(true);
  });

  it('sends a signed-in user with another role to their own home', () => {
    expect(run(roleGuard('ADMIN'), sessionOf('STUDENT'))).toBe('/student');
    expect(run(roleGuard('STUDENT'), sessionOf('PROFESSOR'))).toBe('/professor');
  });
});

describe('guestGuard', () => {
  it('lets a visitor see the login page', () => {
    expect(run(guestGuard, null)).toBe(true);
  });

  it('sends a signed-in user straight to their home', () => {
    expect(run(guestGuard, sessionOf('ADMIN'))).toBe('/admin');
  });
});
