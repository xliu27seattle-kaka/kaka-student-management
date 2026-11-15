import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  effect,
  inject,
  input,
} from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { SemesterForm } from './semester-form';

@Component({
  selector: 'app-semester-fields',
  imports: [ReactiveFormsModule],
  template: `
    <div class="form-grid" [formGroup]="form()">
      <div class="field">
        <label for="semester-code">Code</label>
        <input
          class="control"
          id="semester-code"
          formControlName="code"
          placeholder="2027FA"
          [attr.aria-invalid]="bad('code')"
        />
        @if (bad('code')) {
          <span class="field-error">{{
            form().controls.code.errors?.['server'] ?? 'Year and term: SP, SU or FA, like 2027FA.'
          }}</span>
        }
      </div>
      <div class="field">
        <label for="semester-name">Name</label>
        <input
          class="control"
          id="semester-name"
          formControlName="name"
          placeholder="Fall 2027"
          [attr.aria-invalid]="bad('name')"
        />
        @if (bad('name')) {
          <span class="field-error">Required.</span>
        }
      </div>
      <div class="field">
        <label for="semester-start">Starts</label>
        <input
          class="control"
          id="semester-start"
          type="date"
          formControlName="startDate"
          [attr.aria-invalid]="bad('startDate')"
        />
      </div>
      <div class="field">
        <label for="semester-end">Ends</label>
        <input
          class="control"
          id="semester-end"
          type="date"
          formControlName="endDate"
          [attr.aria-invalid]="bad('endDate') || dateOrder()"
        />
        @if (dateOrder()) {
          <span class="field-error">The end date must be after the start date.</span>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SemesterFields {
  private readonly changes = inject(ChangeDetectorRef);

  readonly form = input.required<SemesterForm>();

  constructor() {
    // The parent marks fields touched or sets server errors; redraw when the form says so.
    effect((onCleanup) => {
      const subscription = this.form().events.subscribe(() => this.changes.markForCheck());
      onCleanup(() => subscription.unsubscribe());
    });
  }

  protected bad(name: 'code' | 'name' | 'startDate' | 'endDate'): boolean {
    const control = this.form().controls[name];
    return control.invalid && control.touched;
  }

  protected dateOrder(): boolean {
    return this.form().hasError('dateOrder') && this.form().controls.endDate.touched;
  }
}
