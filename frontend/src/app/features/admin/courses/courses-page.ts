import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, debounceTime, switchMap } from 'rxjs';
import { toLoadState } from '../../../core/api/load-state';
import { CommonApi } from '../../../shared/api/common-api';
import { EmptyState } from '../../../shared/ui/empty-state';
import { SkeletonRows } from '../../../shared/ui/skeleton-rows';

interface Filter {
  q: string;
  active: boolean | '';
  page: number;
}

@Component({
  selector: 'app-courses-page',
  imports: [AsyncPipe, RouterLink, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Catalogue</span>
          <h1>Courses</h1>
          <p>What can be taught. Sections in each semester are built from these.</p>
        </div>
        <a class="btn btn-primary" routerLink="/admin/courses/new"
          ><i class="ph ph-plus" aria-hidden="true"></i>New course</a
        >
      </header>
      <div class="toolbar rise" style="--i: 1">
        <div class="input">
          <i class="ph ph-magnifying-glass" aria-hidden="true"></i>
          <input
            type="search"
            placeholder="Search by code or title"
            aria-label="Search courses"
            (input)="set({ q: $any($event.target).value })"
          />
        </div>
        <select
          class="control"
          aria-label="Show"
          (change)="
            set({
              active: $any($event.target).value === '' ? '' : $any($event.target).value === 'true',
            })
          "
        >
          <option value="">Active and retired</option>
          <option value="true">Active only</option>
          <option value="false">Retired only</option>
        </select>
      </div>
      <section class="card flush rise" style="--i: 2">
        @if (state.loading) {
          <app-skeleton-rows />
        } @else if (state.error) {
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Courses could not load"
            [message]="state.error.message"
          />
        } @else if (state.data; as page) {
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Course</th>
                  <th>Department</th>
                  <th class="right">Credits</th>
                  <th>Requires</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                @for (c of page.items; track c.id) {
                  <tr>
                    <td>
                      <a class="row-link" [routerLink]="['/admin/courses', c.id]">{{ c.code }}</a>
                      <div class="muted">{{ c.title }}</div>
                    </td>
                    <td>{{ c.department }}</td>
                    <td class="right num">{{ c.credits }}</td>
                    <td>
                      @for (p of c.prerequisites; track p.id) {
                        <span class="badge plain" style="margin-right: 4px">{{ p.code }}</span>
                      } @empty {
                        <span class="muted">None</span>
                      }
                    </td>
                    <td>
                      <span class="badge" [class.ok]="c.active">{{
                        c.active ? 'Active' : 'Retired'
                      }}</span>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5">
                      <app-empty-state icon="ph-books" title="No courses match" />
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          @if (page.totalPages > 1) {
            <nav class="pager" aria-label="Pages">
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                [disabled]="page.page === 0"
                (click)="set({ page: page.page - 1 })"
              >
                Previous
              </button>
              <span class="hint">Page {{ page.page + 1 }} of {{ page.totalPages }}</span>
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                [disabled]="page.page + 1 >= page.totalPages"
                (click)="set({ page: page.page + 1 })"
              >
                Next
              </button>
            </nav>
          }
        }
      </section>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CoursesPage {
  private readonly api = inject(CommonApi);
  private readonly filter = new BehaviorSubject<Filter>({ q: '', active: '', page: 0 });
  protected readonly state$ = this.filter.pipe(
    debounceTime(150),
    switchMap((f) => toLoadState(this.api.courses({ ...f, size: 25 }))),
  );

  protected set(change: Partial<Filter>): void {
    this.filter.next({ ...this.filter.value, page: 0, ...change });
  }
}
