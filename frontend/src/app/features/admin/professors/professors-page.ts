import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, debounceTime, switchMap } from 'rxjs';
import { toLoadState } from '../../../core/api/load-state';
import { AdminApi } from '../../../shared/api/admin-api';
import { EmptyState } from '../../../shared/ui/empty-state';
import { SkeletonRows } from '../../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../../shared/ui/status-badge';

@Component({
  selector: 'app-professors-page',
  imports: [AsyncPipe, RouterLink, StatusBadge, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">People</span>
          <h1>Professors</h1>
          <p>Faculty accounts. Each professor writes their own public profile.</p>
        </div>
        <a class="btn btn-primary" routerLink="/admin/professors/new">
          <i class="ph ph-user-plus" aria-hidden="true"></i>New professor
        </a>
      </header>
      <div class="toolbar rise" style="--i: 1">
        <div class="input">
          <i class="ph ph-magnifying-glass" aria-hidden="true"></i>
          <input
            type="search"
            placeholder="Search by name, email or number"
            aria-label="Search professors"
            (input)="query.next($any($event.target).value)"
          />
        </div>
      </div>
      <section class="card flush rise" style="--i: 2">
        @if (state.loading) {
          <app-skeleton-rows />
        } @else if (state.error) {
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Professors could not load"
            [message]="state.error.message"
          />
        } @else if (state.data; as page) {
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Professor</th>
                  <th>Number</th>
                  <th>Title</th>
                  <th>Account</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (p of page.items; track p.id) {
                  <tr>
                    <td>
                      <a class="row-link" [routerLink]="['/admin/professors', p.id]"
                        >{{ p.firstName }} {{ p.lastName }}</a
                      >
                      <div class="muted">{{ p.email }}</div>
                    </td>
                    <td class="num">{{ p.professorNo }}</td>
                    <td>{{ p.title || '—' }}</td>
                    <td><app-status-badge [status]="p.status" /></td>
                    <td class="right">
                      <a
                        class="icon-btn"
                        [routerLink]="['/admin/professors', p.id]"
                        [attr.aria-label]="'Open ' + p.lastName"
                      >
                        <i class="ph ph-arrow-right" aria-hidden="true"></i>
                      </a>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5">
                      <app-empty-state icon="ph-chalkboard-teacher" title="No professors match" />
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
export class ProfessorsPage {
  private readonly api = inject(AdminApi);
  protected readonly query = new BehaviorSubject('');
  protected readonly state$ = this.query.pipe(
    debounceTime(150),
    switchMap((q) => toLoadState(this.api.professors({ q, size: 100 }))),
  );
}
