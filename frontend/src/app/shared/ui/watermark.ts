import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * A layer of diagonal, repeated "Kaka Studio" text behind a printable document. It is real text,
 * not a background image, because browsers leave background images out when printing.
 */
@Component({
  selector: 'app-watermark',
  template: `
    <div class="mark" aria-hidden="true">
      @for (line of lines; track line) {
        <span>{{ text() }} · {{ text() }} · {{ text() }} · {{ text() }}</span>
      }
    </div>
  `,
  styles: `
    :host {
      position: absolute;
      inset: 0;
      overflow: hidden;
      pointer-events: none;
      z-index: 0;
    }
    .mark {
      position: absolute;
      inset: -40%;
      display: flex;
      flex-direction: column;
      justify-content: space-around;
      transform: rotate(-30deg);
    }
    span {
      white-space: nowrap;
      font: 800 34px/1 var(--font);
      letter-spacing: 0.2em;
      text-transform: uppercase;
      color: var(--ink-1);
      opacity: 0.05;
      print-color-adjust: exact;
      -webkit-print-color-adjust: exact;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Watermark {
  readonly text = input('Kaka Studio');
  protected readonly lines = Array.from({ length: 14 }, (_, i) => i);
}
