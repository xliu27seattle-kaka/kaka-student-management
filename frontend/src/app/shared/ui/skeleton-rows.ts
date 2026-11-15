import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Grey placeholder lines while a list loads. */
@Component({
  selector: 'app-skeleton-rows',
  template: `
    <div class="skeleton-rows" aria-busy="true" aria-label="Loading">
      @for (row of rows(); track row) {
        <div class="skeleton" [style.width.%]="row"></div>
      }
    </div>
  `,
  styles: `
    .skeleton-rows {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding: 24px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SkeletonRows {
  readonly count = input(5);
  protected readonly rows = computed(() =>
    Array.from({ length: this.count() }, (_, i) => 92 - ((i * 17) % 40)),
  );
}
