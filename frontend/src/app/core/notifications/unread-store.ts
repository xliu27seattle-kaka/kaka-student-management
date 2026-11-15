import { Injectable, inject } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CommonApi } from '../../shared/api/common-api';

/**
 * The unread-notifications badge in the shell. The inbox page tells it when messages are read, so
 * the badge and the list always agree.
 */
@Injectable({ providedIn: 'root' })
export class UnreadStore {
  private readonly api = inject(CommonApi);
  private readonly unread = new BehaviorSubject<number>(0);

  readonly unread$ = this.unread.asObservable();

  refresh(): void {
    this.api.unreadCount().subscribe({
      next: (body) => this.unread.next(body.unread),
      error: () => undefined, // the badge is a hint; a failed refresh keeps the last value
    });
  }

  set(count: number): void {
    this.unread.next(Math.max(0, count));
  }

  decrement(): void {
    this.set(this.unread.value - 1);
  }
}
