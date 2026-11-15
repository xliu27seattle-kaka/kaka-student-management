import { Directive, ElementRef, effect, inject, input } from '@angular/core';

/**
 * Counts a number up from zero when it first appears (key figures on dashboards). Keeps the
 * number of decimals of the value; with reduced motion it just shows the value.
 */
@Directive({ selector: '[appCountUp]' })
export class CountUp {
  readonly appCountUp = input<number | null>(null);
  readonly decimals = input(0);
  private readonly element = inject(ElementRef<HTMLElement>);

  constructor() {
    effect((onCleanup) => {
      const target = this.appCountUp();
      const el = this.element.nativeElement as HTMLElement;
      if (target === null) {
        el.textContent = '—';
        return;
      }
      const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
      if (reduce || typeof requestAnimationFrame === 'undefined') {
        el.textContent = target.toFixed(this.decimals());
        return;
      }
      const started = performance.now();
      const duration = 900;
      let frame = 0;
      const tick = (now: number) => {
        const t = Math.min(1, (now - started) / duration);
        const eased = 1 - Math.pow(1 - t, 3);
        el.textContent = (target * eased).toFixed(this.decimals());
        if (t < 1) frame = requestAnimationFrame(tick);
      };
      frame = requestAnimationFrame(tick);
      onCleanup(() => cancelAnimationFrame(frame));
    });
  }
}
