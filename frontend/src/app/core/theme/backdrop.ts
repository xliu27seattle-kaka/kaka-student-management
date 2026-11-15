import { Injectable, inject } from '@angular/core';
import { Observable, combineLatest, distinctUntilChanged, map } from 'rxjs';
import { AuthStore } from '../auth/auth-store';
import { ThemeStore } from './theme-store';

/** Washington DC and Maryland by day (the entry page's photographs), for the light themes. */
export const DAY_PHOTOS: readonly string[] = [
  '/entry/great-falls.jpg',
  '/entry/tidal-basin.jpg',
  '/entry/assateague.jpg',
  '/entry/catoctin.jpg',
  '/entry/canal-towpath.jpg',
];

/** Washington DC at night, for the dark themes. */
export const NIGHT_PHOTOS: readonly string[] = [
  '/night/capitol-night.jpg',
  '/night/jefferson-dusk.jpg',
  '/night/library-of-congress-night.jpg',
];

/**
 * A number that is the same for one sign-in and new at the next, without storing anything: it is
 * derived from the session's token (FNV-1a hash), and every sign-in issues a new token.
 */
export function sessionSeed(token: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < token.length; i++) {
    hash ^= token.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return hash >>> 0;
}

/** One number per session picks the photo; a light or dark theme picks the pool. */
export function backdropFor(seed: number, dark: boolean): string {
  const pool = dark ? NIGHT_PHOTOS : DAY_PHOTOS;
  return pool[seed % pool.length];
}

/**
 * The photograph behind the signed-in pages. It is drawn at random at each sign-in and kept for
 * the whole session (reloads included); switching between a light and a dark theme moves to the
 * matching pool with the same number, so the choice stays stable.
 */
@Injectable({ providedIn: 'root' })
export class BackdropStore {
  private readonly auth = inject(AuthStore);
  private readonly themes = inject(ThemeStore);

  readonly photo$: Observable<string | null> = combineLatest([
    this.auth.session$,
    this.themes.option$,
  ]).pipe(
    map(([session, theme]) =>
      session ? backdropFor(sessionSeed(session.accessToken), theme.dark) : null,
    ),
    distinctUntilChanged(),
  );
}
