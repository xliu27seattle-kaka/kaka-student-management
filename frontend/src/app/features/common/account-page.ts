import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { AuthStore } from '../../core/auth/auth-store';
import { HasUnsavedChanges } from '../../core/guards/unsaved-changes-guard';
import { THEMES, ThemeStore } from '../../core/theme/theme-store';
import { ToastStore } from '../../core/ui/toast-store';
import { CommonApi } from '../../shared/api/common-api';

const sameAsNew: ValidatorFn = (group) =>
  group.value.newPassword && group.value.repeat !== group.value.newPassword
    ? { mismatch: true }
    : null;

const MESSAGES: Record<string, string> = {
  CURRENT_PASSWORD_INCORRECT: 'The current password is not right.',
  PASSWORD_UNCHANGED: 'The new password must be different from the current one.',
};

/** The signed-in person's own account: who they are, the theme, and a new password. */
@Component({
  selector: 'app-account-page',
  imports: [AsyncPipe, ReactiveFormsModule],
  template: `
    @let user = user$ | async;
    @let theme = theme$ | async;
    <div class="page" style="max-width: 920px">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Account</span>
          <h1>{{ user?.firstName }} {{ user?.lastName }}</h1>
          <p>{{ user?.email }}</p>
        </div>
      </header>
      <section class="card rise" style="--i: 1">
        <h2>Theme</h2>
        <div class="themes" role="radiogroup" aria-label="Theme">
          @for (option of themes; track option.id) {
            <button
              type="button"
              role="radio"
              class="theme"
              [attr.aria-checked]="option.id === theme"
              (click)="choose(option.id)"
            >
              <span class="preview" [style.--a]="option.swatch[0]" [style.--b]="option.swatch[1]">
                <span></span><span></span>
              </span>
              <b>{{ option.name }}</b>
              <span class="hint">{{ option.layout === 'top' ? 'Top navigation' : 'Sidebar' }}</span>
            </button>
          }
        </div>
      </section>
      <form class="card rise" style="--i: 2" [formGroup]="form" (ngSubmit)="change()" novalidate>
        <h2>Change password</h2>
        @if (error$ | async; as error) {
          <div class="alert alert-error" role="alert" style="margin-bottom: 16px">{{ error }}</div>
        }
        <div class="form-grid">
          <div class="field span-2">
            <label for="pw-current">Current password</label>
            <input
              class="control"
              id="pw-current"
              type="password"
              autocomplete="current-password"
              formControlName="currentPassword"
            />
          </div>
          <div class="field">
            <label for="pw-new">New password</label>
            <input
              class="control"
              id="pw-new"
              type="password"
              autocomplete="new-password"
              formControlName="newPassword"
              [attr.aria-invalid]="bad('newPassword')"
            />
            <span class="hint" [class.field-error]="bad('newPassword')"
              >At least 8 characters, with a letter and a digit.</span
            >
          </div>
          <div class="field">
            <label for="pw-repeat">Repeat new password</label>
            <input
              class="control"
              id="pw-repeat"
              type="password"
              autocomplete="new-password"
              formControlName="repeat"
              [attr.aria-invalid]="mismatch()"
            />
            @if (mismatch()) {
              <span class="field-error">The two passwords differ.</span>
            }
          </div>
        </div>
        <div class="form-actions">
          <button class="btn btn-primary" type="submit" [disabled]="saving$ | async">
            Change password
          </button>
        </div>
      </form>
    </div>
  `,
  styles: `
    .themes {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 12px;
    }
    .theme {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 4px;
      padding: 12px;
      border: 1.5px solid var(--line);
      border-radius: var(--radius);
      background: var(--paper);
      color: var(--ink-1);
      text-align: left;
      cursor: pointer;
      transition: border-color 0.2s ease-in-out;
      &[aria-checked='true'] {
        border-color: var(--accent);
        box-shadow: 0 0 0 3px var(--accent-soft);
      }
      &:focus-visible {
        outline: 2px solid var(--accent);
        outline-offset: 2px;
      }
    }
    .preview {
      display: grid;
      grid-template-columns: 30% 1fr;
      width: 100%;
      height: 64px;
      margin-bottom: 6px;
      border-radius: 8px;
      overflow: hidden;
      background: var(--a);
      span:first-child {
        background: color-mix(in srgb, var(--a) 80%, #000 20%);
      }
      span:last-child {
        margin: 12px;
        border-radius: 6px;
        background: var(--b);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPage implements HasUnsavedChanges {
  private readonly api = inject(CommonApi);
  private readonly themeStore = inject(ThemeStore);
  private readonly toasts = inject(ToastStore);

  protected readonly user$ = inject(AuthStore).user$;
  protected readonly theme$ = this.themeStore.theme$;
  protected readonly themes = THEMES;
  protected readonly error$ = new BehaviorSubject<string | null>(null);
  protected readonly saving$ = new BehaviorSubject(false);
  protected readonly form = new FormGroup(
    {
      currentPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
      newPassword: new FormControl('', {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
        ],
      }),
      repeat: new FormControl('', { nonNullable: true }),
    },
    { validators: sameAsNew },
  );

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected choose(id: (typeof THEMES)[number]['id']): void {
    this.themeStore.select(id);
  }

  protected bad(name: 'newPassword'): boolean {
    return this.form.controls[name].invalid && this.form.controls[name].touched;
  }

  protected mismatch(): boolean {
    return this.form.hasError('mismatch') && this.form.controls.repeat.touched;
  }

  protected change(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.saving$.next(true);
    this.error$.next(null);
    this.api.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.saving$.next(false);
        this.form.reset();
        this.toasts.success('Password changed.');
      },
      error: (error: unknown) => {
        this.saving$.next(false);
        const apiError = toApiError(error);
        this.error$.next(MESSAGES[apiError.code] ?? apiError.message);
      },
    });
  }
}
