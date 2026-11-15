import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, map, of, switchMap } from 'rxjs';
import { toLoadState } from '../../core/api/load-state';
import { CommonApi } from '../../shared/api/common-api';
import { EmptyState } from '../../shared/ui/empty-state';
import { ProfessorCard } from '../../shared/ui/professor-card';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';

/** A professor's public profile, for any signed-in user (never before sign-in). */
@Component({
  selector: 'app-professor-profile-page',
  imports: [AsyncPipe, ProfessorCard, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      @if (state.loading) {
        <div class="card flush"><app-skeleton-rows /></div>
      } @else if (state.error) {
        <div class="card">
          <app-empty-state
            kind="error"
            icon="ph-user-circle"
            title="This profile is not available"
            [message]="state.error.message"
          />
        </div>
      } @else if (state.data; as d) {
        <header class="page-head rise">
          <div>
            <span class="eyebrow">Faculty</span>
            <h1>{{ d.profile.firstName }} {{ d.profile.lastName }}</h1>
          </div>
        </header>
        <app-professor-card [profile]="d.profile" [sections]="d.sections" />
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfessorProfilePage {
  private readonly api = inject(CommonApi);
  readonly id = input.required<string>();

  protected readonly state$ = toObservable(this.id).pipe(
    switchMap((id) =>
      toLoadState(
        combineLatest([
          this.api.professorProfile(Number(id)),
          this.api.professorSections(Number(id)).pipe(catchError(() => of([]))),
        ]).pipe(map(([profile, sections]) => ({ profile, sections }))),
      ),
    ),
  );
}
