import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, of, throwError, toArray } from 'rxjs';
import { toLoadState } from './load-state';

describe('toLoadState', () => {
  it('reports loading, then the data', async () => {
    const states = await firstValueFrom(toLoadState(of(42)).pipe(toArray()));
    expect(states).toEqual([
      { loading: true, data: null, error: null },
      { loading: false, data: 42, error: null },
    ]);
  });

  it('turns a failed request into the API error shape', async () => {
    const failure = new HttpErrorResponse({
      status: 404,
      error: { code: 'COURSE_NOT_FOUND', message: 'Course not found', fieldErrors: [] },
    });
    const states = await firstValueFrom(toLoadState(throwError(() => failure)).pipe(toArray()));
    expect(states[1].error?.code).toBe('COURSE_NOT_FOUND');
    expect(states[1].loading).toBe(false);
  });
});
