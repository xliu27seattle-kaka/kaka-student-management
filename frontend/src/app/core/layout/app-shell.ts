import { AsyncPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  inject,
  viewChild,
} from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { combineLatest, map } from 'rxjs';
import { ConfirmDialog } from '../../shared/ui/confirm-dialog';
import { ToastStack } from '../../shared/ui/toast-stack';
import { AuthStore } from '../auth/auth-store';
import { UnreadStore } from '../notifications/unread-store';
import { THEMES, ThemeId, ThemeStore } from '../theme/theme-store';
import { AREA_LABEL, NAVIGATION } from './navigation';

/**
 * The frame around every signed-in page: navigation for the user's role (a sidebar, or a top bar
 * in Graphite & Lime), the theme switcher, the unread badge, and the one confirmation modal and
 * toast stack the pages share.
 */
@Component({
  selector: 'app-shell',
  imports: [AsyncPipe, RouterOutlet, RouterLink, RouterLinkActive, ConfirmDialog, ToastStack],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShell implements OnInit, OnDestroy {
  private readonly auth = inject(AuthStore);
  private readonly themes = inject(ThemeStore);
  private readonly unread = inject(UnreadStore);
  private readonly router = inject(Router);
  private readonly themeMenu = viewChild<ElementRef<HTMLDetailsElement>>('themeMenu');

  protected readonly themeOptions = THEMES;
  protected readonly vm$ = combineLatest([
    this.auth.user$,
    this.themes.option$,
    this.unread.unread$,
  ]).pipe(
    map(([user, theme, unread]) => ({
      user,
      theme,
      unread,
      nav: user ? NAVIGATION[user.role] : [],
      area: user ? AREA_LABEL[user.role] : '',
      accountLink: user ? `/${user.role.toLowerCase()}/account` : '/',
      initials: user ? user.firstName.charAt(0) + user.lastName.charAt(0) : '',
    })),
  );

  ngOnInit(): void {
    this.themes.attach();
    if (this.auth.snapshot?.user.role !== 'ADMIN') this.unread.refresh();
  }

  ngOnDestroy(): void {
    this.themes.detach();
  }

  protected chooseTheme(id: ThemeId): void {
    this.themes.select(id);
    const menu = this.themeMenu()?.nativeElement;
    if (menu) menu.open = false;
  }

  protected signOut(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
