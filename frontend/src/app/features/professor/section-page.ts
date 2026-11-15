import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, Observable, filter, finalize, shareReplay, switchMap, tap } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { toLoadState } from '../../core/api/load-state';
import {
  HasUnsavedChanges,
  UNSAVED_CHANGES_QUESTION,
} from '../../core/guards/unsaved-changes-guard';
import { ConfirmStore } from '../../core/ui/confirm-store';
import { ToastStore } from '../../core/ui/toast-store';
import { ProfessorApi } from '../../shared/api/professor-api';
import { RosterEntry, SectionDetail } from '../../shared/api/models';
import { EmptyState } from '../../shared/ui/empty-state';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { StatusBadge } from '../../shared/ui/status-badge';
import { TabItem, Tabs } from '../../shared/ui/tabs';
import { letterFor } from '../../shared/ui/grade-scale';
import { meetingSummary } from '../../shared/ui/week-layout';

type ScoreControl = FormControl<number | null>;

/**
 * One of the professor's sections in three tabs: Roster, Grades (while the semester runs),
 * Message (to everyone enrolled). Leaving a tab or the page with unsaved grades asks first.
 */
@Component({
  selector: 'app-professor-section-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    RouterLink,
    Tabs,
    StatusBadge,
    EmptyState,
    SkeletonRows,
  ],
  templateUrl: './section-page.html',
  styleUrl: './section-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfessorSectionPage implements HasUnsavedChanges {
  private readonly api = inject(ProfessorApi);
  private readonly confirm = inject(ConfirmStore);
  private readonly toasts = inject(ToastStore);

  readonly id = input.required<string>();

  protected readonly tabs: TabItem[] = [
    { id: 'roster', label: 'Roster' },
    { id: 'grades', label: 'Grades' },
    { id: 'message', label: 'Message the class' },
  ];
  protected readonly tab$ = new BehaviorSubject('roster');
  private readonly reload = new BehaviorSubject<void>(undefined);
  protected readonly grades = new FormArray<ScoreControl>([]);
  /** Enrollment ids in the same order as the score controls. */
  protected gradeRows: RosterEntry[] = [];
  protected readonly saving$ = new BehaviorSubject(false);
  protected readonly sending$ = new BehaviorSubject(false);
  protected readonly message = new FormGroup({
    subject: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(150)],
    }),
    body: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(4000)],
    }),
  });

  protected readonly state$ = this.reload.pipe(
    switchMap(() => toLoadState(this.api.section(Number(this.id())))),
    tap((state) => state.data && this.fillGrades(state.data)),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly leaveTab = (): boolean | Observable<boolean> =>
    this.hasUnsavedChanges() ? this.confirm.ask(UNSAVED_CHANGES_QUESTION) : true;

  hasUnsavedChanges(): boolean {
    return this.grades.dirty || this.message.dirty;
  }

  protected selectTab(tab: string): void {
    if (this.hasUnsavedChanges()) {
      this.message.reset();
      this.grades.markAsPristine();
      this.reload.next();
    }
    this.tab$.next(tab);
  }

  private fillGrades(detail: SectionDetail): void {
    this.gradeRows = detail.roster.filter((r) => r.status === 'ENROLLED');
    this.grades.clear();
    for (const row of this.gradeRows) {
      this.grades.push(
        new FormControl<number | null>(row.score, [
          Validators.min(0),
          Validators.max(100),
          Validators.pattern(/^\d+$/),
        ]),
      );
    }
    if (detail.section.semesterStatus !== 'STARTED') this.grades.disable();
    this.grades.markAsPristine();
  }

  protected preview(index: number): string {
    const value = this.grades.at(index).value;
    return value === null ||
      value === undefined ||
      (value as unknown) === '' ||
      this.grades.at(index).invalid
      ? '—'
      : letterFor(Number(value));
  }

  protected missing(): number {
    return this.grades.controls.filter((c) => c.value === null || (c.value as unknown) === '')
      .length;
  }

  protected summary(detail: SectionDetail): string {
    return meetingSummary(detail.section.meetings);
  }

  protected saveGrades(): void {
    if (this.grades.invalid) {
      this.grades.markAllAsTouched();
      return;
    }
    const grades = this.gradeRows.map((row, i) => {
      const value = this.grades.at(i).value;
      return {
        enrollmentId: row.enrollmentId,
        score: value === null || (value as unknown) === '' ? null : Number(value),
      };
    });
    this.saving$.next(true);
    this.api.saveGrades(Number(this.id()), grades).subscribe({
      next: () => {
        this.saving$.next(false);
        this.grades.markAsPristine();
        this.toasts.success('Grades saved.');
        this.reload.next();
      },
      error: (error: unknown) => {
        this.saving$.next(false);
        this.toasts.error(toApiError(error).message);
        // the class list may have changed (a withdrawal); reload so a retry can work
        this.grades.markAsPristine();
        this.reload.next();
      },
    });
  }

  protected send(detail: SectionDetail): void {
    if (this.message.invalid) {
      this.message.markAllAsTouched();
      return;
    }
    const { subject, body } = this.message.getRawValue();
    this.confirm
      .ask({
        title: `Send to ${detail.section.code}?`,
        message: `Every student enrolled in ${detail.section.code} gets "${subject}" in their inbox and by email.`,
        confirmLabel: 'Send',
      })
      .pipe(
        filter(Boolean),
        tap(() => this.sending$.next(true)),
        switchMap(() => this.api.notifySection(detail.section.id, subject, body)),
        finalize(() => this.sending$.next(false)),
      )
      .subscribe({
        next: (sent) => {
          this.message.reset();
          this.toasts.success(`Sent to ${sent.recipientCount} students.`);
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }
}
