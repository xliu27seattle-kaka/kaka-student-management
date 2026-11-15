import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { HasUnsavedChanges } from '../../../core/guards/unsaved-changes-guard';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { PersonFields } from './person-fields';
import { applyServerErrors, personForm } from './person-form';

/**
 * Creates a student or a professor (the route says which). The account is active at once and the
 * person gets a temporary password by email.
 */
@Component({
  selector: 'app-person-form-page',
  imports: [AsyncPipe, ReactiveFormsModule, RouterLink, PersonFields],
  template: `
    @let error = error$ | async;
    <div class="page narrow">
      <header class="page-head rise">
        <div>
          <a class="back-link" [routerLink]="listLink"
            ><i class="ph ph-arrow-left" aria-hidden="true"></i>{{ plural }}</a
          >
          <h1>New {{ kind }}</h1>
          <p>
            The account is active straight away. A temporary password goes to this email address.
          </p>
        </div>
      </header>
      <form class="card rise" style="--i: 1" [formGroup]="form" (ngSubmit)="save()" novalidate>
        @if (error) {
          <div class="alert alert-error" role="alert">
            <i class="ph ph-warning-circle" aria-hidden="true"></i>{{ error }}
          </div>
        }
        <app-person-fields [group]="form" [prefix]="kind" />
        @if (kind === 'professor') {
          <div class="field" style="margin-top: 16px">
            <label for="professor-title">Title</label>
            <input
              class="control"
              id="professor-title"
              [formControl]="title"
              placeholder="Associate Professor of Mathematics"
            />
            <span class="hint"
              >The professor can write the rest of their profile after signing in.</span
            >
          </div>
        }
        <div class="form-actions">
          <a class="btn btn-ghost" [routerLink]="listLink">Cancel</a>
          <button class="btn btn-primary" type="submit" [disabled]="saving$ | async">
            <i class="ph ph-paper-plane-tilt" aria-hidden="true"></i>Create and send password
          </button>
        </div>
      </form>
    </div>
  `,
  styles: `
    .narrow {
      max-width: 760px;
    }
    .alert {
      margin-bottom: 16px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonFormPage implements HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastStore);

  protected readonly kind: 'student' | 'professor' =
    inject(ActivatedRoute).snapshot.data['kind'] ?? 'student';
  protected readonly plural = this.kind === 'student' ? 'Students' : 'Professors';
  protected readonly listLink = `/admin/${this.kind}s`;
  protected readonly form = personForm();
  protected readonly title = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(120)],
  });
  protected readonly saving$ = new BehaviorSubject(false);
  protected readonly error$ = new BehaviorSubject<string | null>(null);
  private saved = false;

  hasUnsavedChanges(): boolean {
    return !this.saved && (this.form.dirty || this.title.dirty);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving$.next(true);
    const person = this.form.getRawValue();
    const request: Observable<{ id: number }> =
      this.kind === 'student'
        ? this.api.createStudent(person)
        : this.api.createProfessor({
            ...person,
            profile: this.title.value
              ? { title: this.title.value, bio: '', expertise: [], experience: [], awards: [] }
              : null,
          });
    request.subscribe({
      next: (created) => {
        this.saved = true;
        this.toasts.success(
          `${person.firstName} ${person.lastName} was added and emailed a password.`,
        );
        void this.router.navigate([this.listLink, created.id]);
      },
      error: (error: unknown) => {
        this.saving$.next(false);
        this.error$.next(applyServerErrors(this.form, toApiError(error)));
      },
    });
  }
}
