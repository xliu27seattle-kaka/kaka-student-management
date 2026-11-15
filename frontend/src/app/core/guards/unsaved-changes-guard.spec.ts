import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable, firstValueFrom, isObservable } from 'rxjs';
import { ConfirmStore } from '../ui/confirm-store';
import { HasUnsavedChanges, unsavedChangesGuard } from './unsaved-changes-guard';

function run(component: HasUnsavedChanges) {
  return TestBed.runInInjectionContext(() =>
    unsavedChangesGuard(
      component,
      {} as ActivatedRouteSnapshot,
      {} as RouterStateSnapshot,
      {} as RouterStateSnapshot,
    ),
  );
}

describe('unsavedChangesGuard', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('lets a clean page go without asking', () => {
    expect(run({ hasUnsavedChanges: () => false })).toBe(true);
  });

  it('asks before leaving unsaved changes, and stays when told to', async () => {
    const result = run({ hasUnsavedChanges: () => true });
    expect(isObservable(result)).toBe(true);

    const answer = firstValueFrom(result as Observable<boolean>);
    const confirm = TestBed.inject(ConfirmStore);
    expect((await firstValueFrom(confirm.request$))?.title).toBe('Leave without saving?');
    confirm.answer(false);
    expect(await answer).toBe(false);
  });
});
