import { Observable, catchError, map, of, startWith } from 'rxjs';
import { ApiError, toApiError } from './api-error';

/** What a page shows while data loads: a skeleton, the data, or an error. */
export interface LoadState<T> {
  loading: boolean;
  data: T | null;
  error: ApiError | null;
}

/** Wraps a request so a template can show loading, data and error states from one stream. */
export function toLoadState<T>(source: Observable<T>): Observable<LoadState<T>> {
  return source.pipe(
    map((data) => ({ loading: false, data, error: null })),
    startWith({ loading: true, data: null, error: null } as LoadState<T>),
    catchError((error: unknown) => of({ loading: false, data: null, error: toApiError(error) })),
  );
}
