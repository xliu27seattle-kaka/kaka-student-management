import { Injectable, inject } from '@angular/core';
import {
  BehaviorSubject,
  Observable,
  Subject,
  catchError,
  finalize,
  map,
  of,
  switchMap,
} from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { ToastStore } from '../../core/ui/toast-store';
import { Offering, Semester } from '../../shared/api/models';
import { StudentApi } from '../../shared/api/student-api';
import { ScheduleEntry } from '../../shared/ui/week-layout';

export interface EnrollmentState {
  semester: Semester | null;
  offerings: Offering[];
  loading: boolean;
  error: string | null;
  /** The section the student is looking at, drawn on the timetable as a dashed block. */
  previewId: number | null;
  /** The section an enroll or drop request is running for. */
  busyId: number | null;
}

const INITIAL: EnrollmentState = {
  semester: null,
  offerings: [],
  loading: false,
  error: null,
  previewId: null,
  busyId: null,
};

/**
 * The state of the enrollment page, shared by its parts: the list of sections, the weekly
 * timetable and the credit summary all read from here, so enrolling in one place updates the others
 * at once. Provided by the page, so it lives exactly as long as the page.
 */
@Injectable()
export class EnrollmentStore {
  private readonly api = inject(StudentApi);
  private readonly toasts = inject(ToastStore);
  private readonly state = new BehaviorSubject<EnrollmentState>(INITIAL);
  /** Every request to reload goes through here; switchMap drops an answer that is out of date. */
  private readonly loads = new Subject<number>();

  readonly state$ = this.state.asObservable();

  /** My sections this semester, plus the one being previewed. */
  readonly schedule$: Observable<ScheduleEntry[]> = this.state$.pipe(
    map((s) => {
      const entries: ScheduleEntry[] = s.offerings
        .filter((o) => o.myStatus === 'ENROLLED')
        .map((o) => entry(o, 'enrolled'));
      const preview = s.offerings.find(
        (o) => o.section.id === s.previewId && o.myStatus !== 'ENROLLED',
      );
      return preview ? [...entries, entry(preview, 'preview')] : entries;
    }),
  );

  readonly credits$: Observable<number> = this.state$.pipe(
    map((s) =>
      s.offerings
        .filter((o) => o.myStatus === 'ENROLLED')
        .reduce((sum, o) => sum + o.section.credits, 0),
    ),
  );

  readonly counts$ = this.state$.pipe(
    map((s) => ({
      enrolled: s.offerings.filter((o) => o.myStatus === 'ENROLLED').length,
      available: s.offerings.filter((o) => o.available).length,
      total: s.offerings.length,
    })),
  );

  get snapshot(): EnrollmentState {
    return this.state.value;
  }

  select(semester: Semester): void {
    this.patch({ semester, offerings: [], previewId: null, error: null });
    this.load();
  }

  preview(sectionId: number | null): void {
    if (this.state.value.previewId !== sectionId) this.patch({ previewId: sectionId });
  }

  enroll(offering: Offering): void {
    this.run(
      offering,
      this.api.enroll(offering.section.id),
      `Enrolled in ${offering.section.code}.`,
    );
  }

  drop(offering: Offering): void {
    if (offering.myEnrollmentId === null) return;
    const started = offering.section.semesterStatus === 'STARTED';
    this.run(
      offering,
      this.api.drop(offering.myEnrollmentId),
      started ? `Withdrew from ${offering.section.code}.` : `Dropped ${offering.section.code}.`,
    );
  }

  private run(offering: Offering, request: Observable<unknown>, success: string): void {
    this.patch({ busyId: offering.section.id });
    request.pipe(finalize(() => this.patch({ busyId: null }))).subscribe({
      next: () => {
        this.toasts.success(success);
        this.load();
      },
      error: (error: unknown) => {
        this.toasts.error(toApiError(error).message);
        this.load();
      },
    });
  }

  constructor() {
    this.loads
      .pipe(
        switchMap((semesterId) =>
          this.api.offerings(semesterId).pipe(
            map((offerings) => ({ offerings, error: null as string | null })),
            catchError((error: unknown) =>
              of({ offerings: [] as Offering[], error: toApiError(error).message }),
            ),
          ),
        ),
      )
      .subscribe(({ offerings, error }) => this.patch({ offerings, error, loading: false }));
  }

  private load(): void {
    const semester = this.state.value.semester;
    if (!semester) return;
    this.patch({ loading: true });
    this.loads.next(semester.id);
  }

  private patch(change: Partial<EnrollmentState>): void {
    this.state.next({ ...this.state.value, ...change });
  }
}

function entry(offering: Offering, tone: ScheduleEntry['tone']): ScheduleEntry {
  return {
    key: String(offering.section.id),
    code: offering.section.code,
    title: offering.section.courseTitle,
    meetings: offering.section.meetings,
    tone,
  };
}
