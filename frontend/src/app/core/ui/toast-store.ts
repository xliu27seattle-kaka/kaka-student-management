import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

export const TOAST_DURATION_MS = 4500;

/** Short messages after an action ("Course saved"). Any component can show one. */
@Injectable({ providedIn: 'root' })
export class ToastStore {
  private readonly toasts = new BehaviorSubject<Toast[]>([]);
  private nextId = 1;

  readonly toasts$ = this.toasts.asObservable();

  success(message: string): void {
    this.show('success', message);
  }

  error(message: string): void {
    this.show('error', message);
  }

  info(message: string): void {
    this.show('info', message);
  }

  dismiss(id: number): void {
    this.toasts.next(this.toasts.value.filter((toast) => toast.id !== id));
  }

  private show(kind: ToastKind, message: string): void {
    const toast: Toast = { id: this.nextId++, kind, message };
    // keep at most three on screen
    this.toasts.next([...this.toasts.value, toast].slice(-3));
    setTimeout(() => this.dismiss(toast.id), TOAST_DURATION_MS);
  }
}
