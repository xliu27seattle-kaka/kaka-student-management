import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** The message a list shows when it has nothing, or could not load. */
@Component({
  selector: 'app-empty-state',
  template: `
    <div
      class="state"
      [class.error]="kind() === 'error'"
      [attr.role]="kind() === 'error' ? 'alert' : null"
    >
      <i class="ph" [class]="'ph ' + icon()" aria-hidden="true"></i>
      <b>{{ title() }}</b>
      @if (message()) {
        <span>{{ message() }}</span>
      }
      <ng-content />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly message = input<string | null>(null);
  readonly icon = input('ph-tray');
  readonly kind = input<'empty' | 'error'>('empty');
}
