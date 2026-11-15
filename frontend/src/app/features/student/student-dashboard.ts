import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { combineLatest, map } from 'rxjs';
import { toLoadState } from '../../core/api/load-state';
import { AuthStore } from '../../core/auth/auth-store';
import { UnreadStore } from '../../core/notifications/unread-store';
import { Enrollment } from '../../shared/api/models';
import { StudentApi } from '../../shared/api/student-api';
import { CountUp } from '../../shared/ui/count-up';
import { EmptyState } from '../../shared/ui/empty-state';
import { ScheduleGrid } from '../../shared/ui/schedule-grid';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { ScheduleEntry } from '../../shared/ui/week-layout';

/** The student's overview: GPA, progress to graduation, this term's week. */
@Component({
  selector: 'app-student-dashboard',
  imports: [AsyncPipe, RouterLink, CountUp, ScheduleGrid, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Overview</span>
          <h1>Welcome back, {{ (user$ | async)?.firstName }}.</h1>
          <p>Your record at a glance. Everything here updates as soon as grades are final.</p>
        </div>
        <a class="btn btn-primary" routerLink="/student/enroll"
          ><i class="ph ph-plus-circle" aria-hidden="true"></i>Enroll</a
        >
      </header>
      @if (state.loading) {
        <div class="card flush"><app-skeleton-rows /></div>
      } @else if (state.error) {
        <div class="card">
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Your overview could not load"
            [message]="state.error.message"
          />
        </div>
      } @else if (state.data; as d) {
        <section class="kpis" aria-label="Key figures">
          <a class="kpi rise" style="--i: 1" routerLink="/student/transcript">
            <span class="kpi-label">Cumulative GPA</span>
            <span
              class="kpi-value"
              [appCountUp]="d.gpa"
              [decimals]="2"
              data-testid="dashboard-gpa"
            ></span>
            <span class="kpi-note">{{
              d.gpa === null ? 'No final grades yet' : 'on a 4.0 scale'
            }}</span>
          </a>
          <a class="kpi rise" style="--i: 2" routerLink="/student/graduation">
            <span class="kpi-label">Earned credits</span>
            <span class="kpi-value"
              ><span [appCountUp]="d.earned"></span
              ><small class="of"> / {{ d.required }}</small></span
            >
            <div class="meter" aria-hidden="true"><span [style.width.%]="d.progress"></span></div>
          </a>
          <a class="kpi rise" style="--i: 3" routerLink="/student/courses">
            <span class="kpi-label">Enrolled now</span>
            <span class="kpi-value" [appCountUp]="d.current.length"></span>
            <span class="kpi-note">sections this term</span>
          </a>
          <a class="kpi rise" style="--i: 4" routerLink="/student/inbox">
            <span class="kpi-label">Unread</span>
            <span class="kpi-value" [appCountUp]="(unread$ | async) ?? 0"></span>
            <span class="kpi-note">notifications</span>
          </a>
        </section>
        <section class="card rise" style="--i: 5">
          <div class="card-head">
            <h2>This term's week</h2>
            <a class="link" routerLink="/student/courses">My courses</a>
          </div>
          <app-schedule-grid [entries]="d.week" />
        </section>
      }
    </div>
  `,
  styles: `
    .of {
      font-size: 16px;
      color: var(--ink-3);
    }
    .meter {
      margin-top: 8px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentDashboard {
  private readonly api = inject(StudentApi);
  protected readonly user$ = inject(AuthStore).user$;
  protected readonly unread$ = inject(UnreadStore).unread$;

  protected readonly state$ = toLoadState(
    combineLatest([this.api.transcript(), this.api.enrollments(), this.api.graduation()]).pipe(
      map(([transcript, enrollments, graduation]) => {
        const current = currentTerm(enrollments);
        return {
          gpa: transcript.cumulativeGpa,
          earned: graduation.earnedCredits,
          required: graduation.requiredCredits,
          progress: Math.min(100, (graduation.earnedCredits / graduation.requiredCredits) * 100),
          current,
          week: current.map((e): ScheduleEntry => ({
            key: String(e.id),
            code: e.section.code,
            title: e.section.courseTitle,
            meetings: e.section.meetings,
            tone: 'enrolled',
          })),
        };
      }),
    ),
  );
}

/** Seats in the running semester, or in the open one if nothing runs yet. */
function currentTerm(enrollments: Enrollment[]): Enrollment[] {
  const active = enrollments.filter((e) => e.status === 'ENROLLED');
  const running = active.filter((e) => e.section.semesterStatus === 'STARTED');
  return running.length ? running : active.filter((e) => e.section.semesterStatus === 'READY');
}
