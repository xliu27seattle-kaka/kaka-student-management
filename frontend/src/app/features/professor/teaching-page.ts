import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { toLoadState } from '../../core/api/load-state';
import { AuthStore } from '../../core/auth/auth-store';
import { ProfessorApi } from '../../shared/api/professor-api';
import { Section } from '../../shared/api/models';
import { EmptyState } from '../../shared/ui/empty-state';
import { ScheduleGrid } from '../../shared/ui/schedule-grid';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../shared/ui/status-badge';
import { ScheduleEntry, meetingSummary } from '../../shared/ui/week-layout';

interface TermGroup {
  name: string;
  status: Section['semesterStatus'];
  sections: Section[];
}

/** What the professor teaches, semester by semester, with this term's week at a glance. */
@Component({
  selector: 'app-teaching-page',
  imports: [AsyncPipe, RouterLink, StatusBadge, EmptyState, SkeletonRows, ScheduleGrid],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Teaching</span>
          <h1>Hello, {{ (user$ | async)?.firstName }}.</h1>
          <p>
            Your sections by semester. Open one to see the class list, enter grades or message the
            class.
          </p>
        </div>
      </header>
      @if (state.loading) {
        <div class="card flush"><app-skeleton-rows /></div>
      } @else if (state.error) {
        <div class="card">
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Your sections could not load"
            [message]="state.error.message"
          />
        </div>
      } @else if (state.data; as d) {
        @if (d.week.length) {
          <section class="card rise" style="--i: 1">
            <h2>This term's week</h2>
            <app-schedule-grid [entries]="d.week" />
          </section>
        }
        @for (term of d.terms; track term.name; let i = $index) {
          <section class="rise" [style.--i]="i + 2">
            <div class="term-title">
              <h2>{{ term.name }}</h2>
              <app-status-badge [status]="term.status" />
            </div>
            <div class="section-cards">
              @for (s of term.sections; track s.id) {
                <a class="section-card" [routerLink]="['/professor/sections', s.id]">
                  <div class="top">
                    <b>{{ s.code }}</b>
                    <span class="hint num">{{ s.enrolledCount }}/{{ s.capacity }}</span>
                  </div>
                  <span class="title">{{ s.courseTitle }}</span>
                  <span class="hint">{{ summary(s) }}{{ s.room ? ' · ' + s.room : '' }}</span>
                  <div class="meter" aria-hidden="true">
                    <span [style.width.%]="(s.enrolledCount / s.capacity) * 100"></span>
                  </div>
                </a>
              }
            </div>
          </section>
        } @empty {
          <div class="card">
            <app-empty-state
              icon="ph-chalkboard-teacher"
              title="No sections yet"
              message="When the registrar assigns you a section it appears here."
            />
          </div>
        }
      }
    </div>
  `,
  styles: `
    .term-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      h2 {
        font: 700 18px/24px var(--font);
        color: var(--ink-1);
      }
    }
    .section-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 12px;
    }
    .section-card {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 18px;
      border-radius: var(--radius-lg);
      background: var(--bg);
      border: 1px solid var(--line);
      box-shadow: var(--shadow);
      color: inherit;
      text-decoration: none;
      transition:
        transform 0.3s var(--ease),
        border-color 0.2s ease-in-out;
      &:hover {
        transform: translateY(-2px);
        border-color: var(--accent);
      }
      .top {
        display: flex;
        justify-content: space-between;
      }
      b {
        color: var(--ink-1);
        font-size: 16px;
      }
      .title {
        color: var(--ink-1);
      }
      .meter {
        margin-top: 6px;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TeachingPage {
  protected readonly user$ = inject(AuthStore).user$;
  protected readonly state$ = toLoadState(
    inject(ProfessorApi)
      .sections()
      .pipe(map((sections) => ({ terms: groupByTerm(sections), week: thisWeek(sections) }))),
  );

  protected summary(section: Section): string {
    return meetingSummary(section.meetings);
  }
}

function groupByTerm(sections: Section[]): TermGroup[] {
  const groups = new Map<string, TermGroup>();
  for (const s of sections) {
    const group = groups.get(s.semesterName) ?? {
      name: s.semesterName,
      status: s.semesterStatus,
      sections: [],
    };
    group.sections.push(s);
    groups.set(s.semesterName, group);
  }
  return [...groups.values()];
}

/** The running semester's classes, or the open one's if nothing runs yet. */
function thisWeek(sections: Section[]): ScheduleEntry[] {
  const running = sections.filter((s) => s.semesterStatus === 'STARTED');
  const current = running.length ? running : sections.filter((s) => s.semesterStatus === 'READY');
  return current.map((s) => ({
    key: String(s.id),
    code: s.code,
    title: s.courseTitle,
    meetings: s.meetings,
    tone: 'enrolled' as const,
  }));
}
