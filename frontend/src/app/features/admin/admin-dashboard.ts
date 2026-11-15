import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toLoadState } from '../../core/api/load-state';
import { AuthStore } from '../../core/auth/auth-store';
import { AdminApi } from '../../shared/api/admin-api';
import { CountUp } from '../../shared/ui/count-up';
import { EmptyState } from '../../shared/ui/empty-state';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../shared/ui/status-badge';

@Component({
  selector: 'app-admin-dashboard',
  imports: [AsyncPipe, DatePipe, RouterLink, CountUp, StatusBadge, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Administration</span>
          <h1>Good to see you, {{ (user$ | async)?.firstName }}.</h1>
          <p>What needs a decision first, then how the semesters stand.</p>
        </div>
        <div class="actions">
          <a class="btn btn-ghost" routerLink="/admin/semesters">
            <i class="ph ph-calendar-plus" aria-hidden="true"></i>Semesters
          </a>
          <a class="btn btn-primary" routerLink="/admin/courses/new">
            <i class="ph ph-plus" aria-hidden="true"></i>New course
          </a>
        </div>
      </header>

      @if (state.loading) {
        <div class="card flush"><app-skeleton-rows [count]="4" /></div>
      } @else if (state.error) {
        <div class="card">
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="The dashboard could not load"
            [message]="state.error.message"
          />
        </div>
      } @else if (state.data; as d) {
        <section class="kpis" aria-label="Key figures">
          <a
            class="kpi rise"
            style="--i: 1"
            routerLink="/admin/students"
            [queryParams]="{ status: 'PENDING' }"
            [class.attention]="d.pendingStudents > 0"
          >
            <span class="kpi-label">Waiting for approval</span>
            <span class="kpi-value" [appCountUp]="d.pendingStudents"></span>
            <span class="kpi-note">student registrations</span>
          </a>
          <a
            class="kpi rise"
            style="--i: 2"
            routerLink="/admin/graduation"
            [class.attention]="d.pendingGraduations > 0"
          >
            <span class="kpi-label">Graduation requests</span>
            <span class="kpi-value" [appCountUp]="d.pendingGraduations"></span>
            <span class="kpi-note">to approve or reject</span>
          </a>
          <a class="kpi rise" style="--i: 3" routerLink="/admin/students">
            <span class="kpi-label">Active students</span>
            <span class="kpi-value" [appCountUp]="d.activeStudents"></span>
          </a>
          <a class="kpi rise" style="--i: 4" routerLink="/admin/professors">
            <span class="kpi-label">Professors</span>
            <span class="kpi-value" [appCountUp]="d.activeProfessors"></span>
          </a>
          <a class="kpi rise" style="--i: 5" routerLink="/admin/courses">
            <span class="kpi-label">Courses offered</span>
            <span class="kpi-value" [appCountUp]="d.activeCourses"></span>
          </a>
          <a
            class="kpi rise"
            style="--i: 6"
            routerLink="/admin/emails"
            [queryParams]="{ status: 'FAILED' }"
            [class.attention]="d.failedEmails > 0"
          >
            <span class="kpi-label">Emails given up</span>
            <span class="kpi-value" [appCountUp]="d.failedEmails"></span>
            <span class="kpi-note">after every retry failed</span>
          </a>
        </section>

        <section class="card rise" style="--i: 7">
          <div class="card-head">
            <h2>Semesters</h2>
            <a class="link" routerLink="/admin/semesters">Manage</a>
          </div>
          <div class="timeline">
            @for (s of d.semesters; track s.id) {
              <a class="term" [routerLink]="['/admin/semesters', s.id]">
                <div class="term-top">
                  <b>{{ s.name }}</b>
                  <app-status-badge [status]="s.status" />
                </div>
                <span class="hint"
                  >{{ s.startDate | date: 'MMM d' }} – {{ s.endDate | date: 'MMM d, y' }}</span
                >
                <div class="term-figures">
                  <span
                    ><b class="num">{{ s.sectionCount }}</b> sections</span
                  >
                  <span
                    ><b class="num">{{ s.enrolledCount }}</b> enrolled</span
                  >
                </div>
              </a>
            } @empty {
              <app-empty-state
                icon="ph-calendar-blank"
                title="No semesters yet"
                message="Create the first one to start building sections."
              />
            }
          </div>
        </section>
      }
    </div>
  `,
  styles: `
    .timeline {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
      gap: 12px;
    }
    .term {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 16px;
      border: 1px solid var(--line);
      border-radius: var(--radius);
      background: var(--paper);
      color: inherit;
      text-decoration: none;
      transition:
        border-color 0.2s ease-in-out,
        transform 0.3s var(--ease);
      &:hover {
        border-color: var(--line-strong);
        transform: translateY(-2px);
      }
      b {
        color: var(--ink-1);
      }
    }
    .term-top {
      display: flex;
      justify-content: space-between;
      gap: 8px;
      align-items: center;
    }
    .term-figures {
      display: flex;
      gap: 16px;
      margin-top: 4px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboard {
  protected readonly user$ = inject(AuthStore).user$;
  protected readonly state$ = toLoadState(inject(AdminApi).dashboard());
}
