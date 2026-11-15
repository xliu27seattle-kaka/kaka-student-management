import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  BehaviorSubject,
  Observable,
  combineLatest,
  filter,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
} from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { toLoadState } from '../../../core/api/load-state';
import {
  HasUnsavedChanges,
  UNSAVED_CHANGES_QUESTION,
} from '../../../core/guards/unsaved-changes-guard';
import { ConfirmStore } from '../../../core/ui/confirm-store';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { Enrollment, Offering, Student } from '../../../shared/api/models';
import { EmptyState } from '../../../shared/ui/empty-state';
import { SkeletonRows } from '../../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { TabItem, Tabs } from '../../../shared/ui/tabs';
import { TranscriptSheet } from '../../../shared/ui/transcript-sheet';
import { meetingSummary } from '../../../shared/ui/week-layout';
import { PersonFields } from '../people/person-fields';
import { applyServerErrors, personForm } from '../people/person-form';

type Action = 'approve' | 'reject' | 'deactivate' | 'reactivate';

const ACTION_QUESTIONS: Record<
  Action,
  { title: string; message: string; label: string; danger: boolean }
> = {
  approve: {
    title: 'Approve this registration?',
    message: 'The student can sign in at once and gets an email.',
    label: 'Approve',
    danger: false,
  },
  reject: {
    title: 'Reject this registration?',
    message: 'The applicant gets an email and may register again later.',
    label: 'Reject',
    danger: true,
  },
  deactivate: {
    title: 'Deactivate this account?',
    message:
      'The student is signed out at once and cannot sign in until reactivated. Their records stay.',
    label: 'Deactivate',
    danger: true,
  },
  reactivate: {
    title: 'Reactivate this account?',
    message: 'The student can sign in again.',
    label: 'Reactivate',
    danger: false,
  },
};

/**
 * One student, in three tabs: Profile (editable), Enrollments (enroll or drop on their behalf,
 * under the same rules they face), Transcript.
 */
@Component({
  selector: 'app-student-detail-page',
  imports: [
    AsyncPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    Tabs,
    StatusBadge,
    EmptyState,
    SkeletonRows,
    TranscriptSheet,
    PersonFields,
  ],
  templateUrl: './student-detail-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentDetailPage implements HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly confirm = inject(ConfirmStore);
  private readonly toasts = inject(ToastStore);
  private readonly router = inject(Router);

  /** From the route, thanks to component input binding. */
  readonly id = input.required<string>();

  protected readonly tabs: TabItem[] = [
    { id: 'profile', label: 'Profile' },
    { id: 'enrollments', label: 'Enrollments' },
    { id: 'transcript', label: 'Transcript' },
  ];
  protected readonly tab$ = new BehaviorSubject('profile');
  private readonly reload = new BehaviorSubject<void>(undefined);
  protected readonly form = personForm();
  protected readonly formError$ = new BehaviorSubject<string | null>(null);
  protected readonly saving$ = new BehaviorSubject(false);

  protected readonly student$ = this.reload.pipe(
    switchMap(() => toLoadState(this.api.student(Number(this.id())))),
    tap((state) => this.fillForm(state.data)),
    shareReplay({ bufferSize: 1, refCount: true }),
  );
  protected readonly enrollments$ = combineLatest([this.tab$, this.reload]).pipe(
    filter(([tab]) => tab === 'enrollments'),
    switchMap(() => toLoadState(this.api.studentEnrollments(Number(this.id())))),
  );
  protected readonly transcript$ = this.tab$.pipe(
    filter((tab) => tab === 'transcript'),
    switchMap(() => toLoadState(this.api.studentTranscript(Number(this.id())))),
  );
  protected readonly semesters$ = this.api
    .semesters()
    .pipe(map((list) => list.filter((s) => s.status === 'READY')));
  protected readonly offeringSemester$ = new BehaviorSubject<number | null>(null);
  protected readonly offerings$ = combineLatest([this.offeringSemester$, this.reload]).pipe(
    switchMap(([semesterId]) =>
      semesterId === null
        ? of(null)
        : toLoadState(this.api.studentOfferings(Number(this.id()), semesterId)),
    ),
  );

  /** Puts the loaded student into the form, unless the administrator is editing it. */
  private fillForm(student: Student | null): void {
    if (student && !this.form.dirty) {
      this.form.reset({
        email: student.email,
        firstName: student.firstName,
        lastName: student.lastName,
      });
    }
  }

  readonly leaveTab = (): boolean | Observable<boolean> =>
    this.hasUnsavedChanges() ? this.confirm.ask(UNSAVED_CHANGES_QUESTION) : true;

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected selectTab(tab: string): void {
    if (this.form.dirty) {
      this.discardChanges();
    }
    this.tab$.next(tab);
  }

  protected discardChanges(): void {
    this.form.markAsPristine();
    this.reload.next();
  }

  protected save(student: Student): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving$.next(true);
    this.formError$.next(null);
    this.api.updateStudent(student.id, this.form.getRawValue()).subscribe({
      next: () => {
        this.saving$.next(false);
        this.form.markAsPristine();
        this.toasts.success('Profile saved.');
        this.reload.next();
      },
      error: (error: unknown) => {
        this.saving$.next(false);
        this.formError$.next(applyServerErrors(this.form, toApiError(error)));
      },
    });
  }

  protected act(student: Student, action: Action): void {
    const q = ACTION_QUESTIONS[action];
    this.confirm
      .ask({ title: q.title, message: q.message, confirmLabel: q.label, danger: q.danger })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.studentAction(student.id, action)),
      )
      .subscribe({
        next: () => {
          this.toasts.success('Done.');
          this.reload.next();
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }

  protected remove(student: Student): void {
    this.confirm
      .ask({
        title: 'Delete this student?',
        message: `${student.firstName} ${student.lastName} and their account are removed for good. A student with any enrollment history cannot be deleted; deactivate them instead.`,
        confirmLabel: 'Delete',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deleteStudent(student.id)),
      )
      .subscribe({
        next: () => {
          this.form.markAsPristine();
          this.toasts.success('Student deleted.');
          void this.router.navigateByUrl('/admin/students');
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }

  protected chooseSemester(value: string): void {
    this.offeringSemester$.next(value ? Number(value) : null);
  }

  protected enroll(offering: Offering): void {
    this.api.enrollStudent(Number(this.id()), offering.section.id).subscribe({
      next: () => {
        this.toasts.success(`Enrolled in ${offering.section.code}.`);
        this.reload.next();
      },
      error: (error: unknown) => this.toasts.error(toApiError(error).message),
    });
  }

  protected drop(enrollment: Enrollment): void {
    const started = enrollment.section.semesterStatus === 'STARTED';
    this.confirm
      .ask({
        title: started ? 'Withdraw from this section?' : 'Drop this section?',
        message: started
          ? `The semester has started, so ${enrollment.section.code} shows as W on the transcript. It does not count in GPA.`
          : `${enrollment.section.code} is removed and the seat is free again.`,
        confirmLabel: started ? 'Withdraw' : 'Drop',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.dropStudent(Number(this.id()), enrollment.id)),
      )
      .subscribe({
        next: () => {
          this.toasts.success(started ? 'Withdrawn.' : 'Dropped.');
          this.reload.next();
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }

  protected summary(enrollment: Enrollment | Offering): string {
    return meetingSummary(enrollment.section.meetings);
  }

  protected reasonList(offering: Offering): string {
    return offering.reasons.map((reason) => reason.message).join(' · ');
  }

  protected print(): void {
    window.print();
  }
}
