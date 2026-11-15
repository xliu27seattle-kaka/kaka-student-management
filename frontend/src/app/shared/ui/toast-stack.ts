import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastStore } from '../../core/ui/toast-store';

const ICON = { success: 'ph-check-circle', error: 'ph-warning-circle', info: 'ph-info' };

@Component({
  selector: 'app-toast-stack',
  imports: [AsyncPipe],
  template: `
    <div class="toasts" aria-live="polite">
      @for (toast of toasts$ | async; track toast.id) {
        <div
          class="toast"
          [class]="'toast ' + toast.kind"
          [attr.role]="toast.kind === 'error' ? 'alert' : 'status'"
        >
          <i class="ph" [class]="'ph ' + icon[toast.kind]" aria-hidden="true"></i>
          <span>{{ toast.message }}</span>
          <button type="button" class="icon-btn" aria-label="Dismiss" (click)="dismiss(toast.id)">
            <i class="ph ph-x" aria-hidden="true"></i>
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .toasts {
      position: fixed;
      right: 24px;
      bottom: 24px;
      z-index: 50;
      display: flex;
      flex-direction: column;
      gap: 8px;
      width: min(380px, calc(100vw - 32px));
    }
    .toast {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 10px 10px 14px;
      border-radius: var(--radius);
      background: var(--bg);
      border: 1px solid var(--line);
      box-shadow: var(--shadow-lg), var(--shadow);
      color: var(--ink-1);
      font-weight: 600;
      animation: toast-in 0.35s var(--ease);
      span {
        flex: 1;
      }
      > .ph {
        font-size: 20px;
      }
      &.success > .ph {
        color: var(--ok);
      }
      &.error > .ph {
        color: var(--danger);
      }
      &.info > .ph {
        color: var(--accent);
      }
      .icon-btn {
        width: 28px;
        height: 28px;
        border: 0;
      }
    }
    @keyframes toast-in {
      from {
        opacity: 0;
        transform: translateY(12px);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastStack {
  private readonly store = inject(ToastStore);
  protected readonly toasts$ = this.store.toasts$;
  protected readonly icon = ICON;

  protected dismiss(id: number): void {
    this.store.dismiss(id);
  }
}
