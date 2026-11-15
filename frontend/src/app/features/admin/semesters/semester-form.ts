import { FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';

export type SemesterForm = FormGroup<{
  code: FormControl<string>;
  name: FormControl<string>;
  startDate: FormControl<string>;
  endDate: FormControl<string>;
}>;

/** The end date must come after the start date (checked here and again by the server). */
export const endAfterStart: ValidatorFn = (group) => {
  const { startDate, endDate } = group.value as { startDate?: string; endDate?: string };
  return startDate && endDate && endDate <= startDate ? { dateOrder: true } : null;
};

export function semesterForm(): SemesterForm {
  return new FormGroup(
    {
      code: new FormControl('', {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.pattern(/^\s*[0-9]{4}(SP|SU|FA|sp|su|fa)\s*$/),
        ],
      }),
      name: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(60)],
      }),
      startDate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      endDate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    },
    { validators: endAfterStart },
  );
}
