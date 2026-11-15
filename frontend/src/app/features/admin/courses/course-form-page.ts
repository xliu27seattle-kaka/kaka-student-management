import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, map } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { HasUnsavedChanges } from '../../../core/guards/unsaved-changes-guard';
import { ToastStore } from '../../../core/ui/toast-store';
import { AdminApi } from '../../../shared/api/admin-api';
import { CommonApi } from '../../../shared/api/common-api';
import { CourseDetail } from '../../../shared/api/models';
import { applyServerErrors } from '../people/person-form';
import { PrerequisitePicker } from './prerequisite-picker';

/** Creates a course, or edits one when the route has an id. */
@Component({
  selector: 'app-course-form-page',
  imports: [AsyncPipe, ReactiveFormsModule, RouterLink, PrerequisitePicker],
  templateUrl: './course-form-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourseFormPage implements OnInit, HasUnsavedChanges {
  private readonly admin = inject(AdminApi);
  private readonly common = inject(CommonApi);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastStore);

  /** Present when editing. */
  readonly id = input<string | undefined>(undefined);

  protected readonly form = new FormGroup({
    code: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^\s*[A-Za-z]{2,4}\s*[0-9]{3}\s*$/)],
    }),
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(120)],
    }),
    department: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^\s*[A-Za-z]{2,6}\s*$/)],
    }),
    credits: new FormControl(3, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(6)],
    }),
    active: new FormControl(true, { nonNullable: true }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(2000)],
    }),
    prerequisiteIds: new FormControl<number[]>([], { nonNullable: true }),
  });

  protected readonly catalogue$ = this.common
    .courses({ size: 100 })
    .pipe(map((page) => page.items));
  protected readonly error$ = new BehaviorSubject<string | null>(null);
  protected readonly saving$ = new BehaviorSubject(false);
  protected readonly loaded$ = new BehaviorSubject<CourseDetail | null>(null);
  private saved = false;

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.common.course(Number(id)).subscribe({
        next: (course) => {
          this.loaded$.next(course);
          this.form.reset({
            code: course.code,
            title: course.title,
            department: course.department,
            credits: course.credits,
            active: course.active,
            description: course.description ?? '',
            prerequisiteIds: course.prerequisites.map((p) => p.id),
          });
        },
        error: (error: unknown) => this.error$.next(toApiError(error).message),
      });
    }
  }

  hasUnsavedChanges(): boolean {
    return !this.saved && this.form.dirty;
  }

  protected bad(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving$.next(true);
    this.error$.next(null);
    const request = this.form.getRawValue();
    const id = this.id();
    const call = id
      ? this.admin.updateCourse(Number(id), request)
      : this.admin.createCourse(request);
    call.subscribe({
      next: (course) => {
        this.saved = true;
        this.toasts.success(`${course.code} saved.`);
        void this.router.navigate(['/admin/courses', course.id]);
      },
      error: (error: unknown) => {
        this.saving$.next(false);
        const apiError = toApiError(error);
        if (apiError.code === 'COURSE_CODE_TAKEN') {
          this.form.controls.code.setErrors({ server: apiError.message });
          this.form.controls.code.markAsTouched();
          return;
        }
        this.error$.next(applyServerErrors(this.form, apiError));
      },
    });
  }
}
