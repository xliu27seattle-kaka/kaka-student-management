import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, distinctUntilChanged, map, tap } from 'rxjs';
import { AuthApi } from './auth-api';
import { Role, Session, UserSummary } from './models';

export const SESSION_STORAGE_KEY = 'kaka.session';

/**
 * The signed-in session, held in one BehaviorSubject. Components subscribe to the observables;
 * guards and the HTTP interceptor read the current value through `snapshot` and `token`.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(AuthApi);
  private readonly session = new BehaviorSubject<Session | null>(readStoredSession());

  readonly session$: Observable<Session | null> = this.session.asObservable();
  readonly user$: Observable<UserSummary | null> = this.session$.pipe(
    map((session) => session?.user ?? null),
    distinctUntilChanged(),
  );
  readonly role$: Observable<Role | null> = this.user$.pipe(
    map((user) => user?.role ?? null),
    distinctUntilChanged(),
  );

  get snapshot(): Session | null {
    return this.session.value;
  }

  get token(): string | null {
    return this.session.value?.accessToken ?? null;
  }

  login(email: string, password: string): Observable<Session> {
    return this.api.login(email, password).pipe(tap((session) => this.set(session)));
  }

  logout(): void {
    this.set(null);
  }

  private set(session: Session | null): void {
    if (session) {
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    } else {
      localStorage.removeItem(SESSION_STORAGE_KEY);
    }
    this.session.next(session);
  }
}

/** Reads the session a previous visit left behind, unless it is unreadable or has expired. */
function readStoredSession(): Session | null {
  const stored = localStorage.getItem(SESSION_STORAGE_KEY);
  if (!stored) return null;
  try {
    const session = JSON.parse(stored) as Session;
    const stillValid =
      !!session.accessToken && !!session.user && new Date(session.expiresAt).getTime() > Date.now();
    if (stillValid) return session;
  } catch {
    // fall through: not a session
  }
  localStorage.removeItem(SESSION_STORAGE_KEY);
  return null;
}
