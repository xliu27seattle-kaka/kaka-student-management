import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { statusLabel, statusTone } from './status';

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="'badge ' + tone()">{{ label() }}</span>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadge {
  readonly status = input.required<string>();
  /** Overrides the standard label, for example "Unofficial". */
  readonly text = input<string | null>(null);

  protected readonly tone = computed(() => statusTone(this.status()));
  protected readonly label = computed(() => this.text() ?? statusLabel(this.status()));
}
