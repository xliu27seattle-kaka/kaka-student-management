import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  BehaviorSubject,
  Observable,
  catchError,
  combineLatest,
  filter,
  of,
  shareReplay,
  switchMap,
  tap,
} from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { toLoadState } from '../../../core/api/load-state';
import {
  HasUnsavedChanges,
  UNSAVED_CHANGES_QUESTION,
} from '../../../core/guards/unsaved-changes-guard';
import { ConfirmStore } from '../../../core/ui/confirm-store';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { CommonApi } from '../../../shared/api/common-api';
import { Professor } from '../../../shared/api/models';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ProfessorCard } from '../../../shared/ui/professor-card';
import { SkeletonRows } from '../../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { TabItem, Tabs } from '../../../shared/ui/tabs';
import { PersonFields } from '../people/person-fields';
import { applyServerErrors, personForm } from '../people/person-form';

/** One professor: the public profile and teaching, and the account details an admin may edit. */
@Component({
  selector: 'app-professor-detail-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    RouterLink,
    Tabs,
    StatusBadge,
    EmptyState,
    SkeletonRows,
    ProfessorCard,
    PersonFields,
  ],
  template: `
    @let state = (professor$ | async)!;
    @let tab = (tab$ | async)!;
    <div class="page">
      @if (state.loading && !state.data) {
        <div class="card flush"><app-skeleton-rows /></div>
      } @else if (state.error) {
        <div class="card">
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="This professor could not load"
            [message]="state.error.message"
          />
        </div>
      } @else if (state.data; as p) {
        <header class="page-head rise">
          <div>
            <a class="back-link" routerLink="/admin/professors"
              ><i class="ph ph-arrow-left" aria-hidden="true"></i>Professors</a
            >
            <h1>{{ p.firstName }} {{ p.lastName }}</h1>
            <p>
              <span class="num">{{ p.professorNo }}</span> · {{ p.email }}
            </p>
            <div class="actions" style="margin-top: 10px">
              <app-status-badge [status]="p.status" />
            </div>
          </div>
          <div class="actions">
            @if (p.status === 'ACTIVE') {
              <button type="button" class="btn btn-ghost" (click)="toggle(p, 'deactivate')">
                Deactivate
              </button>
            } @else {
              <button type="button" class="btn btn-ghost" (click)="toggle(p, 'reactivate')">
                Reactivate
              </button>
            }
            <button type="button" class="btn btn-quiet" (click)="remove(p)">
              <i class="ph ph-trash" aria-hidden="true"></i>Delete
            </button>
          </div>
        </header>
        <app-tabs
          [tabs]="tabs"
          [active]="tab"
          [canLeave]="leaveTab"
          (activeChange)="selectTab($event)"
        />
        @if (tab === 'profile') {
          <div
            role="tabpanel"
            animate.enter="tab-enter"
            aria-labelledby="tab-profile"
            id="tab-profile-panel"
          >
            @if (profile$ | async; as profile) {
              <app-professor-card [profile]="profile" [sections]="(sections$ | async) ?? []" />
            } @else {
              <div class="card">
                <app-empty-state
                  icon="ph-eye-slash"
                  title="No public profile"
                  message="Inactive professors have no public page."
                />
              </div>
            }
          </div>
        } @else {
          <form
            class="card"
            role="tabpanel"
            animate.enter="tab-enter"
            aria-labelledby="tab-account"
            id="tab-account-panel"
            [formGroup]="form"
            (ngSubmit)="save(p)"
            novalidate
          >
            <h2>Account details</h2>
            @if (formError$ | async; as error) {
              <div class="alert alert-error" role="alert" style="margin-bottom: 16px">
                {{ error }}
              </div>
            }
            <app-person-fields [group]="form" prefix="professor" />
            <div class="form-actions">
              <button type="submit" class="btn btn-primary" [disabled]="!form.dirty">
                Save changes
              </button>
            </div>
          </form>
        }
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfessorDetailPage implements HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly common = inject(CommonApi);
  private readonly confirm = inject(ConfirmStore);
  private readonly toasts = inject(ToastStore);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly tabs: TabItem[] = [
    { id: 'profile', label: 'Profile and teaching' },
    { id: 'account', label: 'Account' },
  ];
  protected readonly tab$ = new BehaviorSubject('profile');
  private readonly reload = new BehaviorSubject<void>(undefined);
  protected readonly form = personForm();
  protected readonly formError$ = new BehaviorSubject<string | null>(null);

  protected readonly professor$ = this.reload.pipe(
    switchMap(() => toLoadState(this.api.professor(Number(this.id())))),
    tap((state) => {
      if (state.data && !this.form.dirty) {
        const { email, firstName, lastName } = state.data;
        this.form.reset({ email, firstName, lastName });
      }
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );
  protected readonly profile$ = combineLatest([this.reload, this.tab$]).pipe(
    filter(([, tab]) => tab === 'profile'),
    switchMap(() =>
      this.common.professorProfile(Number(this.id())).pipe(catchError(() => of(null))),
    ),
  );
  protected readonly sections$ = this.reload.pipe(
    switchMap(() =>
      this.common.professorSections(Number(this.id())).pipe(catchError(() => of([]))),
    ),
  );

  readonly leaveTab = (): boolean | Observable<boolean> =>
    this.form.dirty ? this.confirm.ask(UNSAVED_CHANGES_QUESTION) : true;

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected selectTab(tab: string): void {
    if (this.form.dirty) {
      this.form.markAsPristine();
      this.reload.next();
    }
    this.tab$.next(tab);
  }

  protected save(professor: Professor): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.api.updateProfessor(professor.id, this.form.getRawValue()).subscribe({
      next: () => {
        this.form.markAsPristine();
        this.toasts.success('Account details saved.');
        this.reload.next();
      },
      error: (error: unknown) =>
        this.formError$.next(applyServerErrors(this.form, toApiError(error))),
    });
  }

  protected toggle(professor: Professor, action: 'deactivate' | 'reactivate'): void {
    this.confirm
      .ask({
        title:
          action === 'deactivate' ? 'Deactivate this professor?' : 'Reactivate this professor?',
        message:
          action === 'deactivate'
            ? 'They are signed out at once. A professor teaching in an open or running semester must have those sections reassigned first.'
            : 'They can sign in again and their profile is public again.',
        confirmLabel: action === 'deactivate' ? 'Deactivate' : 'Reactivate',
        danger: action === 'deactivate',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.professorAction(professor.id, action)),
      )
      .subscribe({
        next: () => {
          this.toasts.success('Done.');
          this.reload.next();
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }

  protected remove(professor: Professor): void {
    this.confirm
      .ask({
        title: 'Delete this professor?',
        message:
          'Only a professor who never taught a section can be deleted. Otherwise deactivate the account.',
        confirmLabel: 'Delete',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deleteProfessor(professor.id)),
      )
      .subscribe({
        next: () => {
          this.form.markAsPristine();
          this.toasts.success('Professor deleted.');
          void this.router.navigateByUrl('/admin/professors');
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }
}
