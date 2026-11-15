import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  /** Red confirm button, for deletes and other things that cannot be undone. */
  danger?: boolean;
}

export interface ConfirmRequest extends Required<ConfirmOptions> {
  answer: (confirmed: boolean) => void;
}

/**
 * The one confirmation modal of the app. A component calls `ask(...)` and gets an observable that
 * emits true or false once; the modal component in the shell shows whatever is being asked.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmStore {
  private readonly request = new BehaviorSubject<ConfirmRequest | null>(null);

  readonly request$ = this.request.asObservable();

  ask(options: ConfirmOptions): Observable<boolean> {
    return new Observable<boolean>((subscriber) => {
      // a new question replaces one nobody answered
      this.request.value?.answer(false);
      this.request.next({
        confirmLabel: 'Confirm',
        cancelLabel: 'Cancel',
        danger: false,
        ...options,
        answer: (confirmed) => {
          this.request.next(null);
          subscriber.next(confirmed);
          subscriber.complete();
        },
      });
    });
  }

  answer(confirmed: boolean): void {
    this.request.value?.answer(confirmed);
  }
}
