import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { BehaviorSubject, switchMap, tap } from 'rxjs';
import { toLoadState } from '../../core/api/load-state';
import { UnreadStore } from '../../core/notifications/unread-store';
import { CommonApi } from '../../shared/api/common-api';
import { InboxItem } from '../../shared/api/models';
import { EmptyState } from '../../shared/ui/empty-state';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';

/**
 * Notifications sent to the signed-in user. Opening one marks it read, and the badge in the
 * navigation follows through the shared UnreadStore.
 */
@Component({
  selector: 'app-inbox-page',
  imports: [AsyncPipe, DatePipe, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    @let open = open$ | async;
    <div class="page" style="max-width: 920px">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Inbox</span>
          <h1>Notifications</h1>
          <p>Messages from the registrar and your professors. Each one was also emailed to you.</p>
        </div>
        <button type="button" class="btn btn-ghost" (click)="readAll()">
          <i class="ph ph-checks" aria-hidden="true"></i>Mark all read
        </button>
      </header>
      <section class="card flush rise" style="--i: 1">
        @if (state.loading) {
          <app-skeleton-rows [count]="4" />
        } @else if (state.error) {
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Notifications could not load"
            [message]="state.error.message"
          />
        } @else if (state.data; as page) {
          @for (item of page.items; track item.id) {
            <article class="item" [class.unread]="!item.read && !readIds.has(item.id)">
              <button
                type="button"
                class="head"
                [attr.aria-expanded]="open === item.id"
                (click)="toggle(item)"
              >
                <span class="dot" aria-hidden="true"></span>
                <span class="subject">{{ item.subject }}</span>
                <span class="from hint"
                  >{{ item.senderName }}{{ item.sectionCode ? ' · ' + item.sectionCode : '' }}</span
                >
                <span class="when hint">{{ item.createdAt | date: 'MMM d, h:mm a' }}</span>
              </button>
              @if (open === item.id) {
                <p class="body">{{ item.body }}</p>
              }
            </article>
          } @empty {
            <app-empty-state
              icon="ph-bell"
              title="No notifications"
              message="When someone writes to you it shows up here."
            />
          }
        }
      </section>
    </div>
  `,
  styles: `
    .item {
      border-top: 1px solid var(--line);
      &:first-child {
        border-top: 0;
      }
    }
    .head {
      display: grid;
      grid-template-columns: 10px minmax(0, 1fr) auto auto;
      align-items: center;
      gap: 12px;
      width: 100%;
      padding: 16px 24px;
      border: 0;
      background: none;
      color: var(--ink-2);
      text-align: left;
      cursor: pointer;
      &:hover {
        background: var(--panel);
      }
      &:focus-visible {
        outline: 2px solid var(--accent);
        outline-offset: -2px;
      }
      @media (max-width: 720px) {
        grid-template-columns: 10px minmax(0, 1fr);
        .from,
        .when {
          grid-column: 2;
        }
      }
    }
    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    .subject {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .unread {
      .dot {
        background: var(--accent);
      }
      .subject {
        color: var(--ink-1);
        font-weight: 700;
      }
    }
    .body {
      padding: 0 24px 20px 46px;
      white-space: pre-line;
      line-height: 22px;
      animation: rise 0.35s var(--ease);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InboxPage {
  private readonly api = inject(CommonApi);
  private readonly unread = inject(UnreadStore);
  private readonly reload = new BehaviorSubject<void>(undefined);

  protected readonly open$ = new BehaviorSubject<number | null>(null);
  /** Read in this visit, so the list updates without a reload. */
  protected readonly readIds = new Set<number>();
  protected readonly state$ = this.reload.pipe(
    switchMap(() => toLoadState(this.api.inbox())),
    tap(() => this.unread.refresh()),
  );

  protected toggle(item: InboxItem): void {
    this.open$.next(this.open$.value === item.id ? null : item.id);
    if (!item.read && !this.readIds.has(item.id)) {
      this.readIds.add(item.id);
      this.api.markRead(item.id).subscribe(() => this.unread.decrement());
    }
  }

  protected readAll(): void {
    this.api.markAllRead().subscribe(() => {
      this.unread.set(0);
      this.readIds.clear();
      this.reload.next();
    });
  }
}
