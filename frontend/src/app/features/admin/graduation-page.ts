import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, filter, switchMap } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { toLoadState } from '../../core/api/load-state';
import { ConfirmStore } from '../../core/ui/confirm-store';
import { ToastStore } from '../../core/ui/toast-store';
import { AdminApi } from '../../shared/api/admin-api';
import { ApplicationStatus, GraduationApplication } from '../../shared/api/models';
import { EmptyState } from '../../shared/ui/empty-state';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../shared/ui/status-badge';

/** Graduation applications. Approving one graduates the student and issues a certificate. */
@Component({
  selector: 'app-admin-graduation-page',
  imports: [AsyncPipe, DatePipe, RouterLink, StatusBadge, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Graduation</span>
          <h1>Applications</h1>
          <p>A student may apply with 30 earned credits. Both decisions send an email.</p>
        </div>
        <select
          class="control"
          style="width: auto"
          aria-label="Show applications"
          (change)="status.next($any($event.target).value)"
        >
          <option value="PENDING">Waiting for a decision</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="">All</option>
        </select>
      </header>
      <section class="card flush rise" style="--i: 1">
        @if (state.loading) {
          <app-skeleton-rows [count]="3" />
        } @else if (state.error) {
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Applications could not load"
            [message]="state.error.message"
          />
        } @else {
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Student</th>
                  <th class="right">Earned credits</th>
                  <th>Applied</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (a of state.data; track a.id) {
                  <tr>
                    <td>
                      <a class="row-link" [routerLink]="['/admin/students', a.studentId]">{{
                        a.studentName
                      }}</a>
                      <div class="muted">{{ a.studentNo }} · {{ a.email }}</div>
                    </td>
                    <td class="right num">{{ a.earnedCredits }}</td>
                    <td class="muted">{{ a.appliedAt | date: 'medium' }}</td>
                    <td><app-status-badge [status]="a.status" /></td>
                    <td class="right">
                      @if (a.status === 'PENDING') {
                        <div class="actions" style="justify-content: flex-end">
                          <button
                            type="button"
                            class="btn btn-sm btn-ghost"
                            (click)="decide(a, 'reject')"
                          >
                            Reject
                          </button>
                          <button
                            type="button"
                            class="btn btn-sm btn-primary"
                            (click)="decide(a, 'approve')"
                          >
                            Approve
                          </button>
                        </div>
                      } @else {
                        <span class="muted">{{ a.decidedAt | date: 'mediumDate' }}</span>
                      }
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5">
                      <app-empty-state
                        icon="ph-graduation-cap"
                        title="Nothing here"
                        message="No applications with this status."
                      />
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminGraduationPage {
  private readonly api = inject(AdminApi);
  private readonly confirm = inject(ConfirmStore);
  private readonly toasts = inject(ToastStore);
  private readonly reload = new BehaviorSubject<void>(undefined);

  protected readonly status = new BehaviorSubject<ApplicationStatus | ''>('PENDING');
  protected readonly state$ = combineLatest([this.status, this.reload]).pipe(
    switchMap(([status]) => toLoadState(this.api.applications(status))),
  );

  protected decide(application: GraduationApplication, decision: 'approve' | 'reject'): void {
    const approve = decision === 'approve';
    this.confirm
      .ask({
        title: approve
          ? `Approve ${application.studentName}?`
          : `Reject ${application.studentName}?`,
        message: approve
          ? 'The student graduates: they get a certificate number, an official transcript and an email. Their enrollments in open semesters are dropped.'
          : 'The student gets an email and can apply again.',
        confirmLabel: approve ? 'Approve' : 'Reject',
        danger: !approve,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.decide(application.id, decision)),
      )
      .subscribe({
        next: () => {
          this.toasts.success(
            approve ? `${application.studentName} has graduated.` : 'Application rejected.',
          );
          this.reload.next();
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }
}
