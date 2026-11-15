import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, filter, map, switchMap } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { HasUnsavedChanges } from '../../../core/guards/unsaved-changes-guard';
import { ConfirmStore } from '../../../core/ui/confirm-store';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { CommonApi } from '../../../shared/api/common-api';
import { DayOfWeek, Meeting, SectionDetail, Semester } from '../../../shared/api/models';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { short } from '../../../shared/ui/week-layout';

type MeetingForm = FormGroup<{
  day: FormControl<DayOfWeek>;
  start: FormControl<string>;
  end: FormControl<string>;
}>;

const DAYS: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
];

/**
 * Creates a section in a draft semester, or edits one. What can change depends on the semester:
 * everything while it is a draft, only capacity and room once enrollment is open, nothing after.
 * The server checks the 10-minute rule against the professor's other classes.
 */
@Component({
  selector: 'app-section-form-page',
  imports: [AsyncPipe, ReactiveFormsModule, RouterLink, StatusBadge],
  templateUrl: './section-form-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionFormPage implements OnInit, HasUnsavedChanges {
  private readonly admin = inject(AdminApi);
  private readonly common = inject(CommonApi);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastStore);
  private readonly confirm = inject(ConfirmStore);

  /** One of the two is in the route: a new section in a semester, or an existing section. */
  readonly semesterId = input<string | undefined>(undefined);
  readonly sectionId = input<string | undefined>(undefined);

  protected readonly days = DAYS;
  protected readonly courses$ = this.common
    .courses({ active: true, size: 100 })
    .pipe(map((page) => page.items));
  protected readonly professors$ = this.admin
    .professors({ size: 100 })
    .pipe(map((page) => page.items.filter((p) => p.status === 'ACTIVE')));

  protected readonly form = new FormGroup({
    courseId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    professorId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    capacity: new FormControl<number | null>(20, {
      validators: [Validators.min(1), Validators.max(200)],
    }),
    room: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(40)] }),
    meetings: new FormArray<MeetingForm>([]),
  });

  protected readonly semester$ = new BehaviorSubject<Semester | null>(null);
  protected readonly detail$ = new BehaviorSubject<SectionDetail | null>(null);
  protected readonly error$ = new BehaviorSubject<string | null>(null);
  protected readonly saving$ = new BehaviorSubject(false);
  private saved = false;

  ngOnInit(): void {
    const sectionId = this.sectionId();
    if (sectionId) {
      this.admin.section(Number(sectionId)).subscribe({
        next: (detail) => this.load(detail),
        error: (error: unknown) => this.error$.next(toApiError(error).message),
      });
    } else {
      this.addMeeting();
      this.admin
        .semester(Number(this.semesterId()))
        .subscribe((semester) => this.semester$.next(semester));
    }
  }

  private load(detail: SectionDetail): void {
    const s = detail.section;
    this.detail$.next(detail);
    this.admin.semester(s.semesterId).subscribe((semester) => this.semester$.next(semester));
    this.form.controls.meetings.clear();
    for (const meeting of s.meetings) this.addMeeting(meeting);
    this.form.patchValue({
      courseId: s.courseId,
      professorId: s.professorId,
      capacity: s.capacity,
      room: s.room ?? '',
    });
    this.form.markAsPristine();
    // what may change depends on where the semester is
    if (s.semesterStatus === 'DRAFT') return;
    this.form.controls.courseId.disable();
    this.form.controls.professorId.disable();
    this.form.controls.meetings.disable();
    if (s.semesterStatus !== 'READY') this.form.disable();
  }

  /** A retired course is not in the list of choices, but an existing section may still use it. */
  protected listed(courses: { id: number }[], id: number): boolean {
    return courses.some((course) => course.id === id);
  }

  hasUnsavedChanges(): boolean {
    return !this.saved && this.form.dirty;
  }

  protected get meetings(): FormArray<MeetingForm> {
    return this.form.controls.meetings;
  }

  protected addMeeting(meeting?: Meeting): void {
    this.meetings.push(
      new FormGroup({
        day: new FormControl<DayOfWeek>(meeting?.day ?? 'MONDAY', { nonNullable: true }),
        start: new FormControl(meeting ? short(meeting.start) : '10:00', {
          nonNullable: true,
          validators: [Validators.required],
        }),
        end: new FormControl(meeting ? short(meeting.end) : '11:15', {
          nonNullable: true,
          validators: [Validators.required],
        }),
      }),
    );
  }

  protected removeMeeting(index: number): void {
    this.meetings.removeAt(index);
    this.form.markAsDirty();
  }

  protected save(): void {
    if (this.form.invalid || this.meetings.length === 0) {
      this.form.markAllAsTouched();
      if (this.meetings.length === 0) this.error$.next('Add at least one weekly meeting.');
      return;
    }
    const value = this.form.getRawValue();
    const request = {
      courseId: value.courseId!,
      professorId: value.professorId!,
      capacity: value.capacity,
      room: value.room,
      meetings: value.meetings,
    };
    this.saving$.next(true);
    this.error$.next(null);
    const sectionId = this.sectionId();
    const call = sectionId
      ? this.admin.updateSection(Number(sectionId), request)
      : this.admin.createSection(Number(this.semesterId()), request);
    call.subscribe({
      next: (section) => {
        this.saved = true;
        this.toasts.success(`${section.code} saved.`);
        void this.router.navigate(['/admin/semesters', section.semesterId]);
      },
      error: (error: unknown) => {
        this.saving$.next(false);
        this.error$.next(toApiError(error).message);
      },
    });
  }

  protected remove(detail: SectionDetail): void {
    this.confirm
      .ask({
        title: `Delete ${detail.section.code}?`,
        message: 'The section is removed from this draft semester.',
        confirmLabel: 'Delete',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.admin.deleteSection(detail.section.id)),
      )
      .subscribe({
        next: () => {
          this.saved = true;
          this.toasts.success('Section deleted.');
          void this.router.navigate(['/admin/semesters', detail.section.semesterId]);
        },
        error: (error: unknown) => this.toasts.error(toApiError(error).message),
      });
  }
}
