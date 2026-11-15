import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { toLoadState } from '../../core/api/load-state';
import { Enrollment } from '../../shared/api/models';
import { StudentApi } from '../../shared/api/student-api';
import { EmptyState } from '../../shared/ui/empty-state';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../shared/ui/status-badge';
import { meetingSummary } from '../../shared/ui/week-layout';

interface Term {
  name: string;
  status: Enrollment['section']['semesterStatus'];
  rows: Enrollment[];
}

@Component({
  selector: 'app-my-courses-page',
  imports: [AsyncPipe, RouterLink, StatusBadge, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">My courses</span>
          <h1>Every section you have taken</h1>
          <p>Newest semester first. Dropped sections are not listed.</p>
        </div>
      </header>
      @if (state.loading) {
        <div class="card flush"><app-skeleton-rows /></div>
      } @else if (state.error) {
        <div class="card">
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Your courses could not load"
            [message]="state.error.message"
          />
        </div>
      } @else {
        @for (term of state.data; track term.name; let i = $index) {
          <section class="card flush rise" [style.--i]="i">
            <div class="card-head pad">
              <h2>{{ term.name }}</h2>
              <app-status-badge [status]="term.status" />
            </div>
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>Section</th>
                    <th>Professor</th>
                    <th>Schedule</th>
                    <th class="right">Credits</th>
                    <th>Status</th>
                    <th class="right">Grade</th>
                  </tr>
                </thead>
                <tbody>
                  @for (e of term.rows; track e.id) {
                    <tr>
                      <td>
                        <span class="strong">{{ e.section.code }}</span>
                        <div class="muted">{{ e.section.courseTitle }}</div>
                      </td>
                      <td>
                        <a
                          class="row-link"
                          [routerLink]="['/student/professors', e.section.professorId]"
                          >{{ e.section.professorName }}</a
                        >
                      </td>
                      <td class="muted">{{ summary(e) }}</td>
                      <td class="right num">{{ e.section.credits }}</td>
                      <td><app-status-badge [status]="e.status" /></td>
                      <td class="right strong">
                        {{ e.status === 'WITHDRAWN' ? 'W' : (e.letterGrade ?? '—') }}
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        } @empty {
          <div class="card">
            <app-empty-state
              icon="ph-books"
              title="No courses yet"
              message="Sections you enroll in appear here."
            >
              <a class="btn btn-primary" routerLink="/student/enroll">Find sections</a>
            </app-empty-state>
          </div>
        }
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyCoursesPage {
  protected readonly state$ = toLoadState(inject(StudentApi).enrollments().pipe(map(groupByTerm)));

  protected summary(enrollment: Enrollment): string {
    return meetingSummary(enrollment.section.meetings);
  }
}

function groupByTerm(enrollments: Enrollment[]): Term[] {
  const terms = new Map<string, Term>();
  for (const e of enrollments) {
    const term = terms.get(e.section.semesterName) ?? {
      name: e.section.semesterName,
      status: e.section.semesterStatus,
      rows: [],
    };
    term.rows.push(e);
    terms.set(e.section.semesterName, term);
  }
  return [...terms.values()];
}
