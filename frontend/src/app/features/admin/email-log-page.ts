import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, debounceTime, switchMap } from 'rxjs';
import { toLoadState } from '../../core/api/load-state';
import { AdminApi } from '../../shared/api/admin-api';
import { EmailStatus } from '../../shared/api/models';
import { EmptyState } from '../../shared/ui/empty-state';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../shared/ui/status-badge';

interface Filter {
  status: EmailStatus | '';
  q: string;
  page: number;
}

/**
 * Every email the system sent or tried to send: to whom, about what, and whether it arrived at
 * the mail server. Bodies are never shown (they may hold a temporary password, and are removed
 * once sent).
 */
@Component({
  selector: 'app-email-log-page',
  imports: [AsyncPipe, DatePipe, StatusBadge, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    @let summary = summary$ | async;
    @let filter = (filter$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Email</span>
          <h1>Email log</h1>
          <p>Each email is retried a few times before it is given up on. Bodies are not kept.</p>
        </div>
      </header>

      @if (summary) {
        <section class="kpis rise" style="--i: 1" aria-label="Delivery">
          <button
            type="button"
            class="kpi pick"
            [class.picked]="filter.status === 'SENT'"
            (click)="set({ status: filter.status === 'SENT' ? '' : 'SENT' })"
          >
            <span class="kpi-label">Sent</span>
            <span class="kpi-value num">{{ summary.sent }}</span>
          </button>
          <button
            type="button"
            class="kpi pick"
            [class.picked]="filter.status === 'PENDING'"
            (click)="set({ status: filter.status === 'PENDING' ? '' : 'PENDING' })"
          >
            <span class="kpi-label">Waiting or retrying</span>
            <span class="kpi-value num">{{ summary.pending }}</span>
          </button>
          <button
            type="button"
            class="kpi pick"
            [class.attention]="summary.failed > 0"
            [class.picked]="filter.status === 'FAILED'"
            (click)="set({ status: filter.status === 'FAILED' ? '' : 'FAILED' })"
          >
            <span class="kpi-label">Given up</span>
            <span class="kpi-value num">{{ summary.failed }}</span>
          </button>
        </section>
      }

      <div class="toolbar rise" style="--i: 2">
        <div class="input">
          <i class="ph ph-magnifying-glass" aria-hidden="true"></i>
          <input
            type="search"
            placeholder="Search recipient or subject"
            aria-label="Search emails"
            (input)="set({ q: $any($event.target).value })"
          />
        </div>
        <select
          class="control"
          aria-label="Delivery status"
          [value]="filter.status"
          (change)="set({ status: $any($event.target).value })"
        >
          <option value="">All</option>
          <option value="SENT">Sent</option>
          <option value="PENDING">Waiting or retrying</option>
          <option value="FAILED">Given up</option>
        </select>
      </div>

      <section class="card flush rise" style="--i: 3">
        @if (state.loading) {
          <app-skeleton-rows />
        } @else if (state.error) {
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="The email log could not load"
            [message]="state.error.message"
          />
        } @else if (state.data; as page) {
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>To</th>
                  <th>Subject</th>
                  <th>Status</th>
                  <th class="right">Tries</th>
                  <th>Queued</th>
                  <th>Sent</th>
                </tr>
              </thead>
              <tbody>
                @for (e of page.items; track e.id) {
                  <tr>
                    <td class="strong">{{ e.recipient }}</td>
                    <td>
                      {{ e.subject }}
                      @if (e.lastError) {
                        <div class="muted error" [title]="e.lastError">{{ e.lastError }}</div>
                      }
                    </td>
                    <td>
                      <app-status-badge
                        [status]="e.status"
                        [text]="
                          e.status === 'PENDING'
                            ? 'Waiting'
                            : e.status === 'FAILED'
                              ? 'Given up'
                              : null
                        "
                      />
                    </td>
                    <td class="right num">{{ e.attempts }}</td>
                    <td class="muted">{{ e.createdAt | date: 'MMM d, h:mm a' }}</td>
                    <td class="muted">{{ e.sentAt ? (e.sentAt | date: 'MMM d, h:mm a') : '—' }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="6">
                      <app-empty-state icon="ph-envelope-simple" title="No emails match" />
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          @if (page.totalPages > 1) {
            <nav class="pager" aria-label="Pages">
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                [disabled]="page.page === 0"
                (click)="set({ page: page.page - 1 })"
              >
                Previous
              </button>
              <span class="hint"
                >Page {{ page.page + 1 }} of {{ page.totalPages }} ·
                {{ page.totalItems }} emails</span
              >
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                [disabled]="page.page + 1 >= page.totalPages"
                (click)="set({ page: page.page + 1 })"
              >
                Next
              </button>
            </nav>
          }
        }
      </section>
    </div>
  `,
  styles: `
    .pick {
      text-align: left;
      cursor: pointer;
      font: inherit;
      transition: border-color 0.2s ease-in-out;
      &:hover {
        border-color: var(--line-strong);
      }
      &.picked {
        border-color: var(--accent);
        box-shadow: 0 0 0 3px var(--accent-soft);
      }
      &:focus-visible {
        outline: 2px solid var(--accent);
        outline-offset: 2px;
      }
    }
    .error {
      max-width: 420px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: var(--danger);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailLogPage {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);

  private readonly filter = new BehaviorSubject<Filter>({
    status: (inject(ActivatedRoute).snapshot.queryParamMap.get('status') as EmailStatus) ?? '',
    q: '',
    page: 0,
  });
  protected readonly filter$ = this.filter.asObservable();
  protected readonly summary$ = this.api.emailSummary();
  protected readonly state$ = this.filter.pipe(
    debounceTime(150),
    switchMap((f) => toLoadState(this.api.emails(f))),
  );

  protected set(change: Partial<Filter>): void {
    const next = { ...this.filter.value, page: 0, ...change };
    this.filter.next(next);
    void this.router.navigate([], {
      queryParams: { status: next.status || null },
      replaceUrl: true,
    });
  }
}
