import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LoadState } from '../../core/api/load-state';
import { SentNotification } from '../api/models';
import { EmptyState } from './empty-state';
import { SkeletonRows } from './skeleton-rows';

const AUDIENCE_LABEL: Record<string, string> = {
  ALL_STUDENTS: 'All students',
  ALL_PROFESSORS: 'All professors',
  EVERYONE: 'Everyone',
  SECTION: 'Section',
};

/** Notifications someone has sent, with how their emails are doing. */
@Component({
  selector: 'app-sent-list',
  imports: [DatePipe, EmptyState, SkeletonRows],
  template: `
    @let s = state();
    <section class="card flush">
      <div class="card-head pad"><h2>Sent</h2></div>
      @if (s.loading) {
        <app-skeleton-rows [count]="3" />
      } @else if (s.error) {
        <app-empty-state
          kind="error"
          icon="ph-warning-circle"
          title="Sent messages could not load"
          [message]="s.error.message"
        />
      } @else {
        @for (n of s.data; track n.id) {
          <article class="sent">
            <div class="top">
              <b>{{ n.subject }}</b>
              <span class="hint">{{ n.createdAt | date: 'medium' }}</span>
            </div>
            <p class="body">{{ n.body }}</p>
            <div class="meta">
              <span class="badge plain">{{ n.sectionCode ?? audience[n.audience] }}</span>
              <span class="hint">{{ n.recipientCount }} recipients</span>
              <span class="badge ok">{{ n.emailsSent }} emailed</span>
              @if (n.emailsPending) {
                <span class="badge warn">{{ n.emailsPending }} sending</span>
              }
              @if (n.emailsFailed) {
                <span class="badge danger">{{ n.emailsFailed }} failed</span>
              }
            </div>
          </article>
        } @empty {
          <app-empty-state icon="ph-paper-plane-tilt" title="Nothing sent yet" />
        }
      }
    </section>
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
    }
    .sent {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 16px 24px;
      border-top: 1px solid var(--line);
    }
    .top {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      b {
        color: var(--ink-1);
      }
    }
    .body {
      white-space: pre-line;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .meta {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SentList {
  readonly state = input.required<LoadState<SentNotification[]>>();
  protected readonly audience = AUDIENCE_LABEL;
}
