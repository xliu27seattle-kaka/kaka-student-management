import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, map } from 'rxjs';

export type ThemeId = 'sage' | 'midnight' | 'graphite' | 'cobalt';

export interface ThemeOption {
  id: ThemeId;
  name: string;
  /** Two colours for the swatch in the switcher. */
  swatch: [string, string];
  /** Dark themes show night photographs behind the pages, light themes day ones. */
  dark: boolean;
}

export const THEMES: readonly ThemeOption[] = [
  { id: 'sage', name: 'Sage Ledger', swatch: ['#eef0ec', '#2f6b4f'], dark: false },
  { id: 'midnight', name: 'Midnight & Amber', swatch: ['#12141c', '#e8a33d'], dark: true },
  { id: 'graphite', name: 'Graphite & Lime', swatch: ['#1f1f1f', '#b6e018'], dark: true },
  { id: 'cobalt', name: 'Cobalt Trust', swatch: ['#111a2e', '#2d5bd8'], dark: false },
];

export const THEME_STORAGE_KEY = 'kaka.theme';
const DEFAULT_THEME: ThemeId = 'sage';

/**
 * The chosen theme, remembered in the browser. A theme changes colours, fonts and corner radii
 * only; the layout is the same in all four. The app shell attaches it to the page while a user
 * is signed in; the sign-in pages always use the default.
 */
@Injectable({ providedIn: 'root' })
export class ThemeStore {
  private readonly document = inject(DOCUMENT);
  private readonly theme = new BehaviorSubject<ThemeId>(readStoredTheme());
  private attached = false;

  readonly theme$: Observable<ThemeId> = this.theme.asObservable();
  readonly option$: Observable<ThemeOption> = this.theme$.pipe(map(optionOf));

  get current(): ThemeId {
    return this.theme.value;
  }

  select(id: ThemeId): void {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, id);
    } catch {
      // storage can be unavailable (private mode); the choice still applies to this visit
    }
    this.theme.next(id);
    if (this.attached) this.apply(id);
  }

  /** Called by the app shell when it appears. */
  attach(): void {
    this.attached = true;
    this.apply(this.theme.value);
  }

  /** Called by the app shell when it goes away (sign out). */
  detach(): void {
    this.attached = false;
    this.document.documentElement.removeAttribute('data-theme');
  }

  private apply(id: ThemeId): void {
    this.document.documentElement.setAttribute('data-theme', id);
  }
}

export function optionOf(id: ThemeId): ThemeOption {
  return THEMES.find((theme) => theme.id === id) ?? THEMES[0];
}

function readStoredTheme(): ThemeId {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (THEMES.some((theme) => theme.id === stored)) return stored as ThemeId;
  } catch {
    // fall through to the default
  }
  return DEFAULT_THEME;
}
