import { ActivatedRouteSnapshot, ViewTransitionInfo } from '@angular/router';

export type TabDirection = 'forward' | 'back' | 'none';
export type TransitionKind = 'enter' | 'leave' | 'page' | 'none';

const ENTRY_PAGES = new Set(['login', 'register']);
const APP_AREAS = new Set(['admin', 'professor', 'student']);

/** Which way a tab switch travels, so the new panel can come in from that side. */
export function tabDirection(tabs: { id: string }[], from: string, to: string): TabDirection {
  const a = tabs.findIndex((tab) => tab.id === from);
  const b = tabs.findIndex((tab) => tab.id === to);
  if (a < 0 || b < 0 || a === b) return 'none';
  return b > a ? 'forward' : 'back';
}

/**
 * How a navigation is animated: into the app after signing in, out of it when signing out, or a
 * quiet page change inside it. Areas are the first segment of the URL.
 */
export function transitionKind(from: string | null, to: string | null): TransitionKind {
  if (from && to && ENTRY_PAGES.has(from) && APP_AREAS.has(to)) return 'enter';
  if (from && to && APP_AREAS.has(from) && ENTRY_PAGES.has(to)) return 'leave';
  if (from && to && APP_AREAS.has(from) && from === to) return 'page';
  return 'none';
}

function areaOf(root: ActivatedRouteSnapshot): string | null {
  return root.firstChild?.routeConfig?.path ?? null;
}

/**
 * Runs for every router navigation that the browser animates with a view transition. It tags the
 * page with the kind of transition, so the CSS in styles/motion.scss picks the right animation,
 * and skips the animation entirely when the visitor asked for reduced motion.
 */
export function tagViewTransition({ transition, from, to }: ViewTransitionInfo): void {
  const kind = transitionKind(areaOf(from), areaOf(to));
  const reduced = globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  if (reduced || kind === 'none') {
    transition.skipTransition();
    return;
  }
  const root = document.documentElement;
  root.dataset['transition'] = kind;
  transition.finished.finally(() => delete root.dataset['transition']);
}
