import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ConfirmStore } from './confirm-store';

describe('ConfirmStore', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('asks, then emits the answer once and closes', async () => {
    const store = TestBed.inject(ConfirmStore);
    const answers: boolean[] = [];
    store
      .ask({ title: 'Delete?', message: 'Sure?', danger: true })
      .subscribe((a) => answers.push(a));

    const request = await firstValueFrom(store.request$);
    expect(request?.title).toBe('Delete?');
    expect(request?.confirmLabel).toBe('Confirm');
    expect(request?.danger).toBe(true);

    store.answer(true);
    expect(answers).toEqual([true]);
    expect(await firstValueFrom(store.request$)).toBeNull();
  });

  it('a new question cancels one nobody answered', () => {
    const store = TestBed.inject(ConfirmStore);
    const first: boolean[] = [];
    store.ask({ title: 'One', message: '' }).subscribe((a) => first.push(a));
    store.ask({ title: 'Two', message: '' }).subscribe();

    expect(first).toEqual([false]);
  });
});
