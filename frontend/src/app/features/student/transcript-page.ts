import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toLoadState } from '../../core/api/load-state';
import { StudentApi } from '../../shared/api/student-api';
import { EmptyState } from '../../shared/ui/empty-state';
import { SkeletonRows } from '../../shared/ui/skeleton-rows';
import { TranscriptSheet } from '../../shared/ui/transcript-sheet';

/** The student's transcript, any time; Print gives a copy with the watermark. */
@Component({
  selector: 'app-transcript-page',
  imports: [AsyncPipe, TranscriptSheet, EmptyState, SkeletonRows],
  template: `
    @let state = (state$ | async)!;
    <div class="page" style="max-width: 1040px">
      <header class="page-head rise no-print">
        <div>
          <span class="eyebrow">Transcript</span>
          <h1>Your academic record</h1>
          <p>Unofficial until you graduate. Print it or save it as a PDF from the print dialog.</p>
        </div>
        <button type="button" class="btn btn-primary" (click)="print()" [disabled]="!state.data">
          <i class="ph ph-printer" aria-hidden="true"></i>Print
        </button>
      </header>
      @if (state.loading) {
        <div class="card flush"><app-skeleton-rows /></div>
      } @else if (state.error) {
        <div class="card">
          <app-empty-state
            kind="error"
            icon="ph-warning-circle"
            title="Your transcript could not load"
            [message]="state.error.message"
          />
        </div>
      } @else if (state.data; as t) {
        <app-transcript-sheet class="rise" style="--i: 1" [transcript]="t" />
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TranscriptPage {
  protected readonly state$ = toLoadState(inject(StudentApi).transcript());

  protected print(): void {
    window.print();
  }
}
