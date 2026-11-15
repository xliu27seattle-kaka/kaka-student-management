import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { TOAST_DURATION_MS, ToastStore } from './toast-store';

describe('ToastStore', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.resetTestingModule();
  });

  afterEach(() => vi.useRealTimers());

  it('shows a message and hides it after a while', async () => {
    const store = TestBed.inject(ToastStore);
    store.success('Saved');
    expect((await firstValueFrom(store.toasts$)).map((t) => t.message)).toEqual(['Saved']);

    vi.advanceTimersByTime(TOAST_DURATION_MS);
    expect(await firstValueFrom(store.toasts$)).toEqual([]);
  });

  it('keeps at most three on screen', async () => {
    const store = TestBed.inject(ToastStore);
    ['1', '2', '3', '4'].forEach((m) => store.info(m));
    expect((await firstValueFrom(store.toasts$)).map((t) => t.message)).toEqual(['2', '3', '4']);
  });
});
