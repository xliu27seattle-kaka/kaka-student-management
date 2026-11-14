import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { AuthApi } from '../../core/auth/auth-api';
import { EntryLayout } from './entry-layout';

/** Same rule as the backend: 8 to 72 characters with at least one letter and one digit. */
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;

type FieldName = 'firstName' | 'lastName' | 'email' | 'password';

interface ViewState {
  submitting: boolean;
  /** A message for the whole form. */
  error: string | null;
  /** Messages the backend attached to single fields. */
  fieldErrors: Partial<Record<FieldName, string>>;
  /** Set once registration succeeded: the new student number. */
  studentNo: string | null;
}

const INITIAL: ViewState = { submitting: false, error: null, fieldErrors: {}, studentNo: null };

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, AsyncPipe, EntryLayout],
  templateUrl: './register-page.html',
  styleUrl: './auth-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPage {
  private readonly api = inject(AuthApi);

  protected readonly form = inject(NonNullableFormBuilder).group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
  });

  private readonly state = new BehaviorSubject<ViewState>(INITIAL);
  protected readonly state$ = this.state.asObservable();

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.state.value.submitting) return;

    this.state.next({ ...INITIAL, submitting: true });
    this.api.register(this.form.getRawValue()).subscribe({
      next: (response) => this.state.next({ ...INITIAL, studentNo: response.studentNo }),
      error: (error: unknown) => {
        const apiError = toApiError(error);
        const fieldErrors: ViewState['fieldErrors'] = {};
        for (const fieldError of apiError.fieldErrors) {
          fieldErrors[fieldError.field as FieldName] = fieldError.message;
        }
        if (apiError.code === 'EMAIL_TAKEN') {
          fieldErrors.email = 'This email address is already registered.';
        }
        const hasFieldErrors = Object.keys(fieldErrors).length > 0;
        this.state.next({
          ...INITIAL,
          fieldErrors,
          error: hasFieldErrors ? null : apiError.message,
        });
      },
    });
  }

  protected invalid(name: FieldName): boolean {
    const control = this.form.controls[name];
    return (control.invalid && control.touched) || !!this.state.value.fieldErrors[name];
  }
}
