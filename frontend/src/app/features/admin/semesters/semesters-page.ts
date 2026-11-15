import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, switchMap } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { toLoadState } from '../../../core/api/load-state';
import { HasUnsavedChanges } from '../../../core/guards/unsaved-changes-guard';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { EmptyState } from '../../../shared/ui/empty-state';
import { SkeletonRows } from '../../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { applyServerErrors } from '../people/person-form';
import { SemesterFields } from './semester-fields';
import { semesterForm } from './semester-form';

/** Every semester, newest first, and a form to start a new one (always as a draft). */
@Component({
  selector: 'app-semesters-page',
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
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Calendar</span>
          <h1>Semesters</h1>
          <p>
            Draft → enrollment open → in session → ended. Each step is yours to take; dates never
            move a semester.
          </p>
        </div>
      </header>
      <div class="grid-main-side">
        <section class="card flush rise" style="--i: 1">
          @if (state.loading) {
            <app-skeleton-rows />
          } @else if (state.error) {
            <app-empty-state
              kind="error"
              icon="ph-warning-circle"
              title="Semesters could not load"
              [message]="state.error.message"
            />
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>Semester</th>
                    <th>Dates</th>
                    <th>Status</th>
                    <th class="right">Sections</th>
                    <th class="right">Enrolled</th>
                  </tr>
                </thead>
                <tbody>
                  @for (s of state.data; track s.id) {
                    <tr>
                      <td>
                        <a class="row-link" [routerLink]="['/admin/semesters', s.id]">{{
                          s.name
                        }}</a>
                        <div class="muted">{{ s.code }}</div>
                      </td>
                      <td class="muted">
                        {{ s.startDate | date: 'MMM d, y' }} – {{ s.endDate | date: 'MMM d, y' }}
                      </td>
                      <td><app-status-badge [status]="s.status" /></td>
                      <td class="right num">{{ s.sectionCount }}</td>
                      <td class="right num">{{ s.enrolledCount }}</td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="5">
                        <app-empty-state icon="ph-calendar-blank" title="No semesters yet" />
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>
        <form class="card rise" style="--i: 2" [formGroup]="form" (ngSubmit)="create()" novalidate>
          <h2>New semester</h2>
          @if (error$ | async; as error) {
            <div class="alert alert-error" role="alert" style="margin-bottom: 12px">
              {{ error }}
            </div>
          }
          <app-semester-fields [form]="form" />
          <div class="form-actions">
            <button class="btn btn-primary" type="submit">
              <i class="ph ph-plus" aria-hidden="true"></i>Create draft
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SemestersPage implements HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastStore);
  private readonly reload = new BehaviorSubject<void>(undefined);

  protected readonly state$ = this.reload.pipe(switchMap(() => toLoadState(this.api.semesters())));
  protected readonly form = semesterForm();
  protected readonly error$ = new BehaviorSubject<string | null>(null);

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.api.createSemester(this.form.getRawValue()).subscribe({
      next: (semester) => {
        this.form.reset();
        this.toasts.success(`${semester.name} created as a draft.`);
        void this.router.navigate(['/admin/semesters', semester.id]);
      },
      error: (error: unknown) => {
        const apiError = toApiError(error);
        if (apiError.code === 'SEMESTER_CODE_TAKEN') {
          this.form.controls.code.setErrors({ server: apiError.message });
          this.form.controls.code.markAsTouched();
          return;
        }
        this.error$.next(applyServerErrors(this.form, apiError));
      },
    });
  }
}
