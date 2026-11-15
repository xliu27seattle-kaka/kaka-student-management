import {
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CourseSummary } from '../../../shared/api/models';

/**
 * Chooses a course's prerequisites from the catalogue. It is a form control (its value is the list
 * of chosen course ids), so the course form treats it like any other field. The course itself is
 * never offered; cycles through other courses are caught by the server and shown on the form.
 */
@Component({
  selector: 'app-prerequisite-picker',
  template: `
    <div class="picker">
      <div class="chosen" aria-live="polite">
        @for (course of chosen(); track course.id) {
          <span class="pill">
            {{ course.code }}
            <button
              type="button"
              [attr.aria-label]="'Remove ' + course.code"
              (click)="toggle(course.id)"
              [disabled]="disabled()"
            >
              <i class="ph ph-x" aria-hidden="true"></i>
            </button>
          </span>
        } @empty {
          <span class="hint">No prerequisites: anyone can enroll.</span>
        }
      </div>
      <div class="input">
        <i class="ph ph-magnifying-glass" aria-hidden="true"></i>
        <input
          type="search"
          placeholder="Find a course to require"
          aria-label="Find a prerequisite"
          [value]="query()"
          (input)="query.set($any($event.target).value)"
          [disabled]="disabled()"
        />
      </div>
      <ul class="options" role="group" aria-label="Courses">
        @for (course of options(); track course.id) {
          <li>
            <label class="option" [class.on]="selected().includes(course.id)">
              <input
                type="checkbox"
                [checked]="selected().includes(course.id)"
                (change)="toggle(course.id)"
                [disabled]="disabled()"
              />
              <b>{{ course.code }}</b>
              <span>{{ course.title }}</span>
              @if (!course.active) {
                <small class="badge plain">Retired</small>
              }
            </label>
          </li>
        } @empty {
          <li class="hint">No course matches.</li>
        }
      </ul>
    </div>
  `,
  styles: `
    .picker {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .chosen {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      min-height: 32px;
      align-items: center;
    }
    .pill {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      height: 30px;
      padding: 0 4px 0 12px;
      border-radius: 999px;
      background: var(--accent-soft);
      color: var(--accent);
      font-weight: 700;
      animation: pop 0.25s var(--ease);
      button {
        display: grid;
        place-items: center;
        width: 22px;
        height: 22px;
        border: 0;
        border-radius: 50%;
        background: none;
        color: inherit;
        cursor: pointer;
        &:hover {
          background: var(--accent-soft);
        }
      }
    }
    .input input {
      height: 40px;
    }
    .options {
      list-style: none;
      margin: 0;
      padding: 4px;
      max-height: 240px;
      overflow-y: auto;
      border: 1px solid var(--line);
      border-radius: var(--radius);
    }
    .option {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 10px;
      border-radius: 8px;
      cursor: pointer;
      b {
        color: var(--ink-1);
        min-width: 72px;
      }
      span {
        flex: 1;
        color: var(--ink-3);
      }
      &:hover,
      &.on {
        background: var(--panel);
      }
      input {
        accent-color: var(--accent);
      }
    }
    @keyframes pop {
      from {
        transform: scale(0.8);
        opacity: 0;
      }
    }
  `,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PrerequisitePicker), multi: true },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrerequisitePicker implements ControlValueAccessor {
  readonly courses = input.required<CourseSummary[]>();
  /** The course being edited, left out of the choices. */
  readonly excludeId = input<number | null>(null);

  protected readonly query = signal('');
  protected readonly selected = signal<number[]>([]);
  protected readonly disabled = signal(false);

  protected readonly chosen = computed(() =>
    this.courses().filter((course) => this.selected().includes(course.id)),
  );
  protected readonly options = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.courses().filter(
      (course) =>
        course.id !== this.excludeId() &&
        (!q || course.code.toLowerCase().includes(q) || course.title.toLowerCase().includes(q)),
    );
  });

  private onChange: (value: number[]) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  writeValue(value: number[] | null): void {
    this.selected.set(value ?? []);
  }

  registerOnChange(fn: (value: number[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled.set(disabled);
  }

  protected toggle(id: number): void {
    const current = this.selected();
    const next = current.includes(id) ? current.filter((x) => x !== id) : [...current, id];
    this.selected.set(next);
    this.onChange(next);
    this.onTouched();
  }
}
