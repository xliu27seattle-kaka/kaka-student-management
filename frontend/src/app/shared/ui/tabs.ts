import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  afterRenderEffect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { Observable, isObservable, of, take } from 'rxjs';
import { TabDirection, tabDirection } from '../../core/motion/transitions';

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
 *
 * Motion: one underline slides to the active tab, and `data-direction` tells the page's panels
 * (class `tab-enter`, styles/motion.scss) which side to come in from. The switch itself is
 * immediate; the animation never holds up a click.
 */
@Component({
  selector: 'app-tabs',
  template: `
    <div class="tabs" role="tablist" #list>
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
      <span class="indicator" #indicator aria-hidden="true"></span>
    </div>
  `,
  host: { '[attr.data-direction]': 'direction()' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Tabs {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);
  private readonly list = viewChild.required<ElementRef<HTMLElement>>('list');
  private readonly indicator = viewChild.required<ElementRef<HTMLElement>>('indicator');
  /** Which way the last switch went; 'none' until the first one. */
  protected readonly direction = signal<TabDirection>('none');
  private readonly layout = signal(0);

  readonly tabs = input.required<TabItem[]>();
  readonly active = input.required<string>();
  readonly canLeave = input<(() => boolean | Observable<boolean>) | null>(null);
  readonly activeChange = output<string>();

  constructor() {
    // Place the underline under the active tab after every render that changes it.
    afterRenderEffect({
      write: () => {
        this.layout();
        const id = this.active();
        const bar = this.indicator().nativeElement;
        const tab = this.list().nativeElement.querySelector<HTMLElement>(`#tab-${id}`);
        if (!tab) {
          bar.style.opacity = '0';
          return;
        }
        bar.style.opacity = '1';
        bar.style.transform = `translateX(${tab.offsetLeft + 12}px) scaleX(${tab.offsetWidth - 24})`;
      },
    });
    // From the second placement on, the underline slides; tab widths can change (fonts load).
    afterNextRender(() => {
      requestAnimationFrame(() => this.indicator().nativeElement.classList.add('moving'));
      if (typeof ResizeObserver === 'undefined') return;
      const observer = new ResizeObserver(() => this.layout.update((n) => n + 1));
      observer.observe(this.list().nativeElement);
      this.destroyRef.onDestroy(() => observer.disconnect());
    });
  }

  protected select(id: string): void {
    if (id === this.active()) return;
    const check = this.canLeave()?.() ?? true;
    const answer = isObservable(check) ? check : of(check);
    answer.pipe(take(1)).subscribe((ok) => {
      if (!ok) return;
      this.direction.set(tabDirection(this.tabs(), this.active(), id));
      this.activeChange.emit(id);
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
