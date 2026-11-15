import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { HasUnsavedChanges } from '../../core/guards/unsaved-changes-guard';
import { ToastStore } from '../../core/ui/toast-store';
import { ProfessorApi } from '../../shared/api/professor-api';
import { Award, Experience } from '../../shared/api/models';

const required = (max: number) => [Validators.required, Validators.maxLength(max)];

/** The professor writes their own introduction, expertise, experience and awards. */
@Component({
  selector: 'app-profile-edit-page',
  imports: [AsyncPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './profile-edit-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileEditPage implements OnInit, HasUnsavedChanges {
  private readonly api = inject(ProfessorApi);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastStore);

  protected readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    bio: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(4000)] }),
    expertise: new FormArray<FormControl<string>>([]),
    experience: new FormArray<ReturnType<typeof experienceGroup>>([]),
    awards: new FormArray<ReturnType<typeof awardGroup>>([]),
  });
  protected readonly newExpertise = new FormControl('', { nonNullable: true });
  protected readonly error$ = new BehaviorSubject<string | null>(null);
  protected readonly saving$ = new BehaviorSubject(false);
  private saved = false;

  ngOnInit(): void {
    this.api.myProfile().subscribe((profile) => {
      this.form.patchValue({ title: profile.title ?? '', bio: profile.bio ?? '' });
      profile.expertise.forEach((label) =>
        this.form.controls.expertise.push(expertiseControl(label)),
      );
      profile.experience.forEach((e) => this.form.controls.experience.push(experienceGroup(e)));
      profile.awards.forEach((a) => this.form.controls.awards.push(awardGroup(a)));
      this.form.markAsPristine();
    });
  }

  hasUnsavedChanges(): boolean {
    return !this.saved && this.form.dirty;
  }

  protected addExpertise(): void {
    const label = this.newExpertise.value.trim();
    if (!label || this.form.controls.expertise.length >= 12) return;
    this.form.controls.expertise.push(expertiseControl(label));
    this.newExpertise.reset();
    this.form.markAsDirty();
  }

  protected addExperience(): void {
    this.form.controls.experience.push(experienceGroup());
    this.form.markAsDirty();
  }

  protected addAward(): void {
    this.form.controls.awards.push(awardGroup());
    this.form.markAsDirty();
  }

  protected removeAt(list: FormArray, index: number): void {
    list.removeAt(index);
    this.form.markAsDirty();
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error$.next('Some entries are incomplete.');
      return;
    }
    this.saving$.next(true);
    this.error$.next(null);
    this.api.updateMyProfile(this.form.getRawValue()).subscribe({
      next: () => {
        this.saved = true;
        this.toasts.success('Profile saved.');
        void this.router.navigateByUrl('/professor/profile');
      },
      error: (error: unknown) => {
        this.saving$.next(false);
        this.error$.next(toApiError(error).message);
      },
    });
  }
}

function expertiseControl(label: string): FormControl<string> {
  return new FormControl(label, { nonNullable: true, validators: required(120) });
}

function experienceGroup(e?: Experience) {
  return new FormGroup({
    roleTitle: new FormControl(e?.roleTitle ?? '', {
      nonNullable: true,
      validators: required(160),
    }),
    organization: new FormControl(e?.organization ?? '', {
      nonNullable: true,
      validators: required(160),
    }),
    period: new FormControl(e?.period ?? '', { nonNullable: true, validators: required(60) }),
  });
}

function awardGroup(a?: Award) {
  return new FormGroup({
    title: new FormControl(a?.title ?? '', { nonNullable: true, validators: required(200) }),
    year: new FormControl(a?.year ?? new Date().getFullYear(), {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1900), Validators.max(2100)],
    }),
  });
}
