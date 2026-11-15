import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  effect,
  inject,
  input,
} from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

/** Email, first and last name: the part of a person form students and professors share. */
@Component({
  selector: 'app-person-fields',
  imports: [ReactiveFormsModule],
  template: `
    <div class="form-grid" [formGroup]="group()">
      <div class="field span-2">
        <label [for]="prefix() + '-email'">Email</label>
        <input
          class="control"
          [id]="prefix() + '-email'"
          type="email"
          formControlName="email"
          autocomplete="off"
          [attr.aria-invalid]="bad('email')"
        />
        @if (bad('email')) {
          <span class="field-error">{{ message('email', 'Enter a valid email address.') }}</span>
        }
      </div>
      <div class="field">
        <label [for]="prefix() + '-first'">First name</label>
        <input
          class="control"
          [id]="prefix() + '-first'"
          formControlName="firstName"
          [attr.aria-invalid]="bad('firstName')"
        />
        @if (bad('firstName')) {
          <span class="field-error">Required.</span>
        }
      </div>
      <div class="field">
        <label [for]="prefix() + '-last'">Last name</label>
        <input
          class="control"
          [id]="prefix() + '-last'"
          formControlName="lastName"
          [attr.aria-invalid]="bad('lastName')"
        />
        @if (bad('lastName')) {
          <span class="field-error">Required.</span>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonFields {
  private readonly changes = inject(ChangeDetectorRef);

  readonly group = input.required<FormGroup>();

  constructor() {
    // The parent marks fields touched or sets server errors; redraw when the form says so.
    effect((onCleanup) => {
      const subscription = this.group().events.subscribe(() => this.changes.markForCheck());
      onCleanup(() => subscription.unsubscribe());
    });
  }
  readonly prefix = input('person');

  protected message(name: string, fallback: string): string {
    return this.group().get(name)?.errors?.['server'] ?? fallback;
  }

  protected bad(name: string): boolean {
    const control = this.group().get(name);
    return !!control && control.invalid && control.touched;
  }
}
