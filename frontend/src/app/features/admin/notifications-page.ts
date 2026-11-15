import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BehaviorSubject, switchMap } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { toLoadState } from '../../core/api/load-state';
import { HasUnsavedChanges } from '../../core/guards/unsaved-changes-guard';
import { ConfirmStore } from '../../core/ui/confirm-store';
import { ToastStore } from '../../core/ui/toast-store';
import { AdminApi } from '../../shared/api/admin-api';
import { Audience } from '../../shared/api/models';
import { SentList } from '../../shared/ui/sent-list';

const AUDIENCES: { value: Audience; label: string }[] = [
  { value: 'ALL_STUDENTS', label: 'All students' },
  { value: 'ALL_PROFESSORS', label: 'All professors' },
  { value: 'EVERYONE', label: 'Everyone' },
];

/** Writes to all students, all professors or everyone; each gets it in the app and by email. */
@Component({
  selector: 'app-admin-notifications-page',
  imports: [AsyncPipe, ReactiveFormsModule, SentList],
  template: `
    <div class="page">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Notifications</span>
          <h1>Write to people</h1>
          <p>Every recipient sees it in their inbox and gets an email.</p>
        </div>
      </header>
      <div class="grid-main-side">
        <app-sent-list class="rise" style="--i: 2" [state]="(sent$ | async)!" />
        <form class="card rise" style="--i: 1" [formGroup]="form" (ngSubmit)="send()" novalidate>
          <h2>New notification</h2>
          <div class="form-grid" style="grid-template-columns: minmax(0, 1fr)">
            <div class="field">
              <label for="note-audience">To</label>
              <select class="control" id="note-audience" formControlName="audience">
                @for (a of audiences; track a.value) {
                  <option [value]="a.value">{{ a.label }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="note-subject">Subject</label>
              <input
                class="control"
                id="note-subject"
                formControlName="subject"
                maxlength="150"
                [attr.aria-invalid]="bad('subject')"
              />
              @if (bad('subject')) {
                <span class="field-error">A subject is needed.</span>
              }
            </div>
            <div class="field">
              <label for="note-body">Message</label>
              <textarea
                class="control"
                id="note-body"
                rows="7"
                formControlName="body"
                maxlength="4000"
                [attr.aria-invalid]="bad('body')"
              ></textarea>
              @if (bad('body')) {
                <span class="field-error">Write a message.</span>
              }
            </div>
          </div>
          <div class="form-actions">
            <button class="btn btn-primary" type="submit" [disabled]="sending$ | async">
              <i class="ph ph-paper-plane-tilt" aria-hidden="true"></i>Send
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminNotificationsPage implements HasUnsavedChanges {
  private readonly api = inject(AdminApi);
  private readonly confirm = inject(ConfirmStore);
  private readonly toasts = inject(ToastStore);
  private readonly reload = new BehaviorSubject<void>(undefined);

  protected readonly audiences = AUDIENCES;
  protected readonly sending$ = new BehaviorSubject(false);
  protected readonly sent$ = this.reload.pipe(
    switchMap(() => toLoadState(this.api.sentNotifications())),
  );
  protected readonly form = new FormGroup({
    audience: new FormControl<Audience>('ALL_STUDENTS', { nonNullable: true }),
    subject: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(150)],
    }),
    body: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(4000)],
    }),
  });

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  protected bad(name: 'subject' | 'body'): boolean {
    return this.form.controls[name].invalid && this.form.controls[name].touched;
  }

  protected send(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const to = AUDIENCES.find((a) => a.value === value.audience)?.label.toLowerCase();
    this.confirm
      .ask({
        title: `Send to ${to}?`,
        message: `"${value.subject}" goes to every inbox and is emailed to each person. It cannot be recalled.`,
        confirmLabel: 'Send',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.sending$.next(true);
        this.api.sendNotification(value).subscribe({
          next: (sent) => {
            this.sending$.next(false);
            this.form.reset({ audience: value.audience, subject: '', body: '' });
            this.toasts.success(`Sent to ${sent.recipientCount} people.`);
            this.reload.next();
          },
          error: (error: unknown) => {
            this.sending$.next(false);
            this.toasts.error(toApiError(error).message);
          },
        });
      });
  }
}
