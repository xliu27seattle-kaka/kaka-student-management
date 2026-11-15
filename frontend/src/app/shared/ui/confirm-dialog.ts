import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  effect,
  inject,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ConfirmStore } from '../../core/ui/confirm-store';

/**
 * Shows whatever the ConfirmStore is asking, in a native modal dialog (focus is trapped and
 * Escape cancels). Lives once in the app shell.
 */
@Component({
  selector: 'app-confirm-dialog',
  template: `
    @let request = current();
    <dialog #dialog class="modal" aria-labelledby="confirm-title" (cancel)="answer(false, $event)">
      @if (request) {
        <div class="modal-body">
          <h2 id="confirm-title">{{ request.title }}</h2>
          <p>{{ request.message }}</p>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-ghost" (click)="answer(false)">
            {{ request.cancelLabel }}
          </button>
          <button
            type="button"
            class="btn"
            [class.btn-danger]="request.danger"
            [class.btn-primary]="!request.danger"
            (click)="answer(true)"
          >
            {{ request.confirmLabel }}
          </button>
        </div>
      }
    </dialog>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  private readonly store = inject(ConfirmStore);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly current = toSignal(this.store.request$, { initialValue: null });

  constructor() {
    effect(() => {
      const dialog = this.dialog().nativeElement;
      if (this.current() && !dialog.open) dialog.showModal();
      if (!this.current() && dialog.open) dialog.close();
    });
  }

  protected answer(confirmed: boolean, event?: Event): void {
    event?.preventDefault();
    this.store.answer(confirmed);
  }
}
