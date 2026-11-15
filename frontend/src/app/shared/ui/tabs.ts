import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  input,
  output,
} from '@angular/core';
import { Observable, isObservable, of, take } from 'rxjs';

export interface TabItem {
  id: string;
  label: string;
}

/**
 * A row of tabs following the WAI-ARIA tabs pattern: only the active tab is in the Tab order, the
 * arrow keys, Home and End move between tabs, and each tab points at its panel
 * (`tab-<id>-panel`, which the page gives the matching element).
 *
 * The parent owns which tab is active (input) and hears about changes (output). `canLeave` lets a
 * tab with an unsaved form ask before the switch, using the confirmation modal.
 */
@Component({
  selector: 'app-tabs',
  template: `
    <div class="tabs" role="tablist">
      @for (tab of tabs(); track tab.id; let i = $index) {
        <button
          type="button"
          role="tab"
          [id]="'tab-' + tab.id"
          [attr.aria-controls]="'tab-' + tab.id + '-panel'"
          [attr.aria-selected]="tab.id === active()"
          [attr.tabindex]="tab.id === active() ? 0 : -1"
          (click)="select(tab.id)"
          (keydown)="onKey($event, i)"
        >
          {{ tab.label }}
        </button>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Tabs {
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly tabs = input.required<TabItem[]>();
  readonly active = input.required<string>();
  readonly canLeave = input<(() => boolean | Observable<boolean>) | null>(null);
  readonly activeChange = output<string>();

  protected select(id: string): void {
    if (id === this.active()) return;
    const check = this.canLeave()?.() ?? true;
    const answer = isObservable(check) ? check : of(check);
    answer.pipe(take(1)).subscribe((ok) => {
      if (ok) this.activeChange.emit(id);
    });
  }

  protected onKey(event: KeyboardEvent, index: number): void {
    const count = this.tabs().length;
    const target =
      event.key === 'ArrowRight'
        ? (index + 1) % count
        : event.key === 'ArrowLeft'
          ? (index - 1 + count) % count
          : event.key === 'Home'
            ? 0
            : event.key === 'End'
              ? count - 1
              : null;
    if (target === null) return;
    event.preventDefault();
    const id = this.tabs()[target].id;
    (this.host.nativeElement as HTMLElement).querySelector<HTMLElement>(`#tab-${id}`)?.focus();
    this.select(id);
  }
}
