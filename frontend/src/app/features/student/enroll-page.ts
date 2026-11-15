import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { BehaviorSubject, combineLatest, filter, map } from 'rxjs';
import { ConfirmStore } from '../../core/ui/confirm-store';
import { CommonApi } from '../../shared/api/common-api';
import { Offering, Semester } from '../../shared/api/models';
import { EmptyState } from '../../shared/ui/empty-state';
import { ScheduleGrid } from '../../shared/ui/schedule-grid';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../shared/ui/status-badge';
import { EnrollmentStore } from './enrollment-store';
import { OfferingRow } from './offering-row';

/**
 * Enrollment: every section of the chosen semester, available ones first, greyed-out ones with
 * the reason. Hovering a section previews it on the weekly timetable.
 */
@Component({
  selector: 'app-enroll-page',
  imports: [AsyncPipe, OfferingRow, ScheduleGrid, StatusBadge, EmptyState, SkeletonRows],
  providers: [EnrollmentStore],
  templateUrl: './enroll-page.html',
  styles: `
    .sticky {
      position: sticky;
      top: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .totals {
      display: flex;
      gap: 24px;
      margin-bottom: 16px;
      div {
        display: flex;
        flex-direction: column;
      }
      b {
        font: 700 24px/30px var(--font-num);
        color: var(--ink-1);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnrollPage implements OnInit {
  private readonly common = inject(CommonApi);
  private readonly confirm = inject(ConfirmStore);
  protected readonly store = inject(EnrollmentStore);

  protected readonly semesters$ = new BehaviorSubject<Semester[] | null>(null);
  protected readonly query$ = new BehaviorSubject('');
  protected readonly onlyAvailable$ = new BehaviorSubject(false);

  protected readonly visible$ = combineLatest([
    this.store.state$,
    this.query$,
    this.onlyAvailable$,
  ]).pipe(
    map(([state, query, onlyAvailable]) => {
      const q = query.trim().toLowerCase();
      return state.offerings
        .filter((o) => !onlyAvailable || o.available || o.myStatus === 'ENROLLED')
        .filter(
          (o) =>
            !q ||
            o.section.code.toLowerCase().includes(q) ||
            o.section.courseTitle.toLowerCase().includes(q) ||
            o.section.professorName.toLowerCase().includes(q),
        )
        .sort((a, b) => rank(a) - rank(b));
    }),
  );

  ngOnInit(): void {
    this.common
      .semesters()
      .pipe(map((list) => list.filter((s) => s.status === 'READY' || s.status === 'STARTED')))
      .subscribe((list) => {
        this.semesters$.next(list);
        // the soonest semester open for enrollment, else the running one
        const open = list
          .filter((s) => s.status === 'READY')
          .sort((a, b) => a.startDate.localeCompare(b.startDate));
        const first = open[0] ?? list[0];
        if (first) this.store.select(first);
      });
  }

  protected choose(id: string): void {
    const semester = this.semesters$.value?.find((s) => s.id === Number(id));
    if (semester) this.store.select(semester);
  }

  protected drop(offering: Offering): void {
    const started = offering.section.semesterStatus === 'STARTED';
    this.confirm
      .ask({
        title: started
          ? `Withdraw from ${offering.section.code}?`
          : `Drop ${offering.section.code}?`,
        message: started
          ? 'The semester has started, so this shows as W on your transcript. It does not count in your GPA. You cannot enroll again this semester.'
          : 'Your seat is given up. You can enroll again while enrollment is open, if a seat is free.',
        confirmLabel: started ? 'Withdraw' : 'Drop',
        danger: true,
      })
      .pipe(filter(Boolean))
      .subscribe(() => this.store.drop(offering));
  }
}

/** Mine first, then the ones I can take, then the rest. */
function rank(offering: Offering): number {
  if (offering.myStatus === 'ENROLLED') return 0;
  return offering.available ? 1 : 2;
}
