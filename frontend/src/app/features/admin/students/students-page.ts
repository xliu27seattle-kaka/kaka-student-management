import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, debounceTime, switchMap } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { toLoadState } from '../../../core/api/load-state';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { Student, UserStatus } from '../../../shared/api/models';
import { EmptyState } from '../../../shared/ui/empty-state';
import { SkeletonRows } from '../../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../../shared/ui/status-badge';

interface Filter {
  status: UserStatus | '';
  q: string;
  page: number;
}

/** Every student, filterable by account status and searchable; pending ones can be decided here. */
@Component({
  selector: 'app-students-page',
  imports: [AsyncPipe, DatePipe, RouterLink, StatusBadge, EmptyState, SkeletonRows],
  templateUrl: './students-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentsPage {
  private readonly api = inject(AdminApi);
  private readonly toasts = inject(ToastStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly statuses: UserStatus[] = ['PENDING', 'ACTIVE', 'INACTIVE', 'REJECTED'];
  protected readonly filter = new BehaviorSubject<Filter>({
    status: (this.route.snapshot.queryParamMap.get('status') as UserStatus) ?? '',
    q: '',
    page: 0,
  });
  private readonly reload = new BehaviorSubject<void>(undefined);

  protected readonly state$ = combineLatest([
    this.filter.pipe(debounceTime(150)),
    this.reload,
  ]).pipe(switchMap(([filter]) => toLoadState(this.api.students(filter))));
  protected readonly filter$ = this.filter.asObservable();

  protected setStatus(status: string): void {
    this.filter.next({ ...this.filter.value, status: status as UserStatus | '', page: 0 });
    void this.router.navigate([], { queryParams: { status: status || null }, replaceUrl: true });
  }

  protected search(q: string): void {
    this.filter.next({ ...this.filter.value, q, page: 0 });
  }

  protected goTo(page: number): void {
    this.filter.next({ ...this.filter.value, page });
  }

  protected decide(student: Student, action: 'approve' | 'reject'): void {
    this.api.studentAction(student.id, action).subscribe({
      next: () => {
        this.toasts.success(
          `${student.firstName} ${student.lastName} ${action === 'approve' ? 'approved' : 'rejected'}. An email is on its way.`,
        );
        this.reload.next();
      },
      error: (error: unknown) => this.toasts.error(toApiError(error).message),
    });
  }
}
