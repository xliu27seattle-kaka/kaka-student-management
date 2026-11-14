import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { AuthStore } from '../../core/auth/auth-store';
import { HOME_BY_ROLE } from '../../core/auth/models';
import { DEMO_ACCOUNTS, DemoAccount } from './demo-accounts';
import { EntryLayout } from './entry-layout';

/** What the visitor is told for each way a login can fail. */
const MESSAGE_BY_CODE: Record<string, string> = {
  BAD_CREDENTIALS: 'Email or password is incorrect.',
  ACCOUNT_PENDING:
    'Your registration is still waiting for approval. You will get an email as soon as it is decided.',
  ACCOUNT_REJECTED:
    'Your registration was not approved. You can register again with the same email address.',
  ACCOUNT_INACTIVE: 'This account has been deactivated. Please contact an administrator.',
  VALIDATION_FAILED: 'Enter a valid email address and your password.',
};

interface ViewState {
  submitting: boolean;
  error: string | null;
}

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, AsyncPipe, EntryLayout],
  templateUrl: './login-page.html',
  styleUrl: './auth-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  private readonly store = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly demoAccounts = DEMO_ACCOUNTS;
  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  private readonly state = new BehaviorSubject<ViewState>({ submitting: false, error: null });
  protected readonly state$ = this.state.asObservable();
  protected readonly showPassword$ = new BehaviorSubject(false);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.state.value.submitting) return;

    const { email, password } = this.form.getRawValue();
    this.state.next({ submitting: true, error: null });
    this.store.login(email, password).subscribe({
      next: (session) => void this.router.navigateByUrl(HOME_BY_ROLE[session.user.role]),
      error: (error: unknown) => {
        const apiError = toApiError(error);
        this.state.next({
          submitting: false,
          error: MESSAGE_BY_CODE[apiError.code] ?? apiError.message,
        });
      },
    });
  }

  protected fill(account: DemoAccount): void {
    this.form.setValue({ email: account.email, password: account.password });
    this.state.next({ submitting: false, error: null });
  }

  protected togglePassword(): void {
    this.showPassword$.next(!this.showPassword$.value);
  }

  /** True once the visitor has left a field that is still invalid. */
  protected invalid(name: 'email' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }
}
