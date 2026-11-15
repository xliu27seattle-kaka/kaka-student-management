import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { BehaviorSubject, combineLatest, filter, shareReplay, switchMap, tap } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { toLoadState } from '../../../core/api/load-state';
import { ConfirmStore } from '../../../core/ui/confirm-store';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { CommonApi } from '../../../shared/api/common-api';
import { CourseDetail, Section } from '../../../shared/api/models';
import { EmptyState } from '../../../shared/ui/empty-state';
import { SkeletonRows } from '../../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { TabItem, Tabs } from '../../../shared/ui/tabs';
import { meetingSummary } from '../../../shared/ui/week-layout';

/** One course in three tabs: Overview, Prerequisites (both directions), Sections. */
@Component({
  selector: 'app-course-detail-page',
  imports: [AsyncPipe, RouterLink, Tabs, StatusBadge, EmptyState, SkeletonRows],
  templateUrl: './course-detail-page.html',
  styleUrl: './course-detail-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourseDetailPage {
  private readonly api = inject(AdminApi);
  private readonly common = inject(CommonApi);
  private readonly confirm = inject(ConfirmStore);
  private readonly toasts = inject(ToastStore);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly tabs: TabItem[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'prerequisites', label: 'Prerequisites' },
    { id: 'sections', label: 'Sections' },
  ];
  protected readonly tab$ = new BehaviorSubject('overview');
  /** Following a prerequisite link reuses this page with another id, so everything follows the id. */
  private readonly id$ = toObservable(this.id);
  protected readonly course$ = this.id$.pipe(
    tap(() => this.tab$.next('overview')),
    switchMap((id) => toLoadState(this.common.course(Number(id)))),
    shareReplay({ bufferSize: 1, refCount: true }),
  );
  protected readonly sections$ = combineLatest([this.id$, this.tab$]).pipe(
    filter(([, tab]) => tab === 'sections'),
    switchMap(([id]) => toLoadState(this.api.courseSections(Number(id)))),
  );

  protected summary(section: Section): string {
    return meetingSummary(section.meetings);
  }

  protected remove(course: CourseDetail): void {
    this.confirm
      .ask({
        title: `Delete ${course.code}?`,
        message:
          'A course with sections, or one other courses require, cannot be deleted. Retire it instead so it gets no new sections.',
        confirmLabel: 'Delete',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deleteCourse(course.id)),
      )
      .subscribe({
        next: () => {
          this.toasts.success(`${course.code} deleted.`);
          void this.router.navigateByUrl('/admin/courses');
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }
}
