import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { catchError, combineLatest, map, of, switchMap } from 'rxjs';
import { toLoadState } from '../../core/api/load-state';
import { CommonApi } from '../../shared/api/common-api';
import { ProfessorApi } from '../../shared/api/professor-api';
import { EmptyState } from '../../shared/ui/empty-state';
import { ProfessorCard } from '../../shared/ui/professor-card';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';

/** The professor's own profile, exactly as students see it, with a way to edit it. */
@Component({
  selector: 'app-my-profile-page',
  imports: [AsyncPipe, ProfessorCard, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">My profile</span>
          <h1>How students see you</h1>
          <p>Every signed-in user can open this page from a section you teach.</p>
        </div>
      </header>
      @if (state.loading) {
        <div class="card flush"><app-skeleton-rows /></div>
      } @else if (state.error) {
        <div class="card">
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Your profile could not load"
            [message]="state.error.message"
          />
        </div>
      } @else if (state.data; as d) {
        <app-professor-card
          [profile]="d.profile"
          [sections]="d.sections"
          editLink="/professor/profile/edit"
        />
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyProfilePage {
  private readonly common = inject(CommonApi);
  protected readonly state$ = toLoadState(
    inject(ProfessorApi)
      .myProfile()
      .pipe(
        switchMap((profile) =>
          combineLatest([
            of(profile),
            this.common.professorSections(profile.id).pipe(catchError(() => of([]))),
          ]),
        ),
        map(([profile, sections]) => ({ profile, sections })),
      ),
  );
}
