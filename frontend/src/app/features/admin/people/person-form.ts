import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ApiError } from '../../../core/api/api-error';

export type PersonForm = FormGroup<{
  email: FormControl<string>;
  firstName: FormControl<string>;
  lastName: FormControl<string>;
}>;

export function personForm(): PersonForm {
  return new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(255)],
    }),
    firstName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    lastName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
  });
}

/**
 * Puts the backend's field errors on the matching controls, so they show next to the field.
 * Returns the message for anything that is not about one field.
 */
export function applyServerErrors(form: FormGroup, error: ApiError): string | null {
  let unplaced = error.fieldErrors.length === 0;
  for (const fieldError of error.fieldErrors) {
    const control = form.get(fieldError.field);
    if (control) {
      control.setErrors({ server: fieldError.message });
      control.markAsTouched();
    } else {
      unplaced = true;
    }
  }
  if (error.code === 'EMAIL_TAKEN') {
    form.get('email')?.setErrors({ server: error.message });
    form.get('email')?.markAsTouched();
    return null;
  }
  return unplaced ? error.message : null;
}
