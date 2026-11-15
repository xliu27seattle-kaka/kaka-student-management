import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, filter, switchMap } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { toLoadState } from '../../core/api/load-state';
import { ConfirmStore } from '../../core/ui/confirm-store';
import { ToastStore } from '../../core/ui/toast-store';
import { StudentApi } from '../../shared/api/student-api';
import { EmptyState } from '../../shared/ui/empty-state';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../shared/ui/status-badge';
import { Watermark } from '../../shared/ui/watermark';

/**
 * Progress to graduation and the application. Once approved, the certificate is here to print,
 * with the same watermark as the transcript.
 */
@Component({
  selector: 'app-student-graduation-page',
  imports: [AsyncPipe, DatePipe, RouterLink, StatusBadge, EmptyState, SkeletonRows, Watermark],
  templateUrl: './graduation-page.html',
  styleUrl: './graduation-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentGraduationPage {
  private readonly api = inject(StudentApi);
  private readonly confirm = inject(ConfirmStore);
  private readonly toasts = inject(ToastStore);
  private readonly reload = new BehaviorSubject<void>(undefined);

  protected readonly state$ = this.reload.pipe(switchMap(() => toLoadState(this.api.graduation())));

  protected apply(): void {
    this.confirm
      .ask({
        title: 'Apply for graduation?',
        message:
          'An administrator reviews your application and you get an email with the decision. Once you graduate you cannot enroll in new sections, and any sections you hold in an open semester are dropped.',
        confirmLabel: 'Apply',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.applyForGraduation()),
      )
      .subscribe({
        next: () => {
          this.toasts.success('Application sent. You will get an email with the decision.');
          this.reload.next();
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }

  protected print(): void {
    window.print();
  }
}
