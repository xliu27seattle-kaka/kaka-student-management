import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, filter, of, shareReplay, switchMap, tap } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { toLoadState } from '../../../core/api/load-state';
import { ConfirmOptions, ConfirmStore } from '../../../core/ui/confirm-store';
import { HasUnsavedChanges } from '../../../core/guards/unsaved-changes-guard';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { CourseSection, Semester, SemesterStatus } from '../../../shared/api/models';
import { EmptyState } from '../../../shared/ui/empty-state';
import { SkeletonRows } from '../../../shared/ui/skeleton-rows';
import { statusLabel } from '../../../shared/ui/status';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { meetingSummary } from '../../../shared/ui/week-layout';
import { applyServerErrors } from '../people/person-form';
import { SemesterFields } from './semester-fields';
import { semesterForm } from './semester-form';

/** What each step means, shown on its button and in the confirmation before it is taken. */
export const TRANSITIONS: Record<
  SemesterStatus,
  { label: string; icon: string; consequence: string }
> = {
  READY: {
    label: 'Open enrollment',
    icon: 'ph-door-open',
    consequence:
      'Students and professors can see the semester and students can enroll. Course, professor and meeting times of its sections are fixed from now on; capacity and room can still change.',
  },
  DRAFT: {
    label: 'Back to draft',
    icon: 'ph-arrow-counter-clockwise',
    consequence:
      'Enrollment closes and the semester is hidden again. This works only while nobody is enrolled.',
  },
  STARTED: {
    label: 'Start the semester',
    icon: 'ph-play',
    consequence:
      'Enrollment closes. Students can still withdraw (shown as W). Professors can enter grades from now on.',
  },
  ENDED: {
    label: 'End the semester',
    icon: 'ph-flag-checkered',
    consequence:
      'Grades become final and count toward credits and GPA. Every enrolled student must have a grade first. This cannot be undone.',
  },
};

@Component({
  selector: 'app-semester-detail-page',
  imports: [
    AsyncPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    StatusBadge,
    EmptyState,
    SkeletonRows,
    SemesterFields,
  ],
  templateUrl: './semester-detail-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SemesterDetailPage implements HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly confirm = inject(ConfirmStore);
  private readonly toasts = inject(ToastStore);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  private readonly reload = new BehaviorSubject<void>(undefined);
  protected readonly transitions = TRANSITIONS;
  protected readonly form = semesterForm();
  protected readonly formError$ = new BehaviorSubject<string | null>(null);
  protected readonly blocked$ = new BehaviorSubject<string | null>(null);

  protected readonly semester$ = this.reload.pipe(
    switchMap(() => toLoadState(this.api.semester(Number(this.id())))),
    tap((state) => {
      if (state.data) {
        const { code, name, startDate, endDate } = state.data;
        this.form.reset({ code, name, startDate, endDate });
        if (state.data.status === 'DRAFT') this.form.controls.code.enable();
        else this.form.controls.code.disable();
      }
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );
  protected readonly sections$ = this.reload.pipe(
    switchMap(() => toLoadState(this.api.sections(Number(this.id())))),
  );

  protected summary(section: CourseSection): string {
    return meetingSummary(section.meetings);
  }

  protected move(semester: Semester, target: SemesterStatus): void {
    this.blocked$.next(null);
    // Before ending, ask the server what is still ungraded so the modal can say so.
    const missing$ =
      target === 'ENDED' ? this.api.ungradedSections(semester.id) : of([] as string[]);
    missing$
      .pipe(
        switchMap((missing) => this.confirm.ask(this.question(semester, target, missing))),
        filter(Boolean),
        switchMap(() => this.api.transition(semester.id, target)),
      )
      .subscribe({
        next: (moved) => {
          this.toasts.success(`${moved.name}: ${statusLabel(moved.status).toLowerCase()}.`);
          this.reload.next();
        },
        error: (error: unknown) => this.blocked$.next(toApiError(error).message),
      });
  }

  private question(semester: Semester, target: SemesterStatus, missing: string[]): ConfirmOptions {
    const step = TRANSITIONS[target];
    const message = missing.length
      ? `Grades are still missing in ${missing.join(', ')}. The semester can end only when every enrolled student has a score.`
      : step.consequence;
    return {
      title: `${step.label}: ${semester.name}?`,
      message,
      confirmLabel: missing.length ? 'Try anyway' : step.label,
      danger: target === 'ENDED' || target === 'DRAFT',
    };
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected save(semester: Semester): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.api.updateSemester(semester.id, { ...this.form.getRawValue() }).subscribe({
      next: () => {
        this.toasts.success('Semester saved.');
        this.reload.next();
      },
      error: (error: unknown) =>
        this.formError$.next(applyServerErrors(this.form, toApiError(error))),
    });
  }

  protected remove(semester: Semester): void {
    this.confirm
      .ask({
        title: `Delete ${semester.name}?`,
        message: 'Only a draft without sections can be deleted.',
        confirmLabel: 'Delete',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deleteSemester(semester.id)),
      )
      .subscribe({
        next: () => {
          this.toasts.success('Semester deleted.');
          void this.router.navigateByUrl('/admin/semesters');
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }
}
