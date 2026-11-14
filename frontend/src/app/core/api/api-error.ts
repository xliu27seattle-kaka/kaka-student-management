import { HttpErrorResponse } from '@angular/common/http';

export interface ApiFieldError {
  field: string;
  message: string;
}

/** The error body every backend endpoint returns. `code` is stable; `message` is for people. */
export interface ApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors: ApiFieldError[];
}

/** Turns anything an HTTP call can fail with into an ApiError, so pages handle one shape. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiError> | null;
    if (body && typeof body === 'object' && typeof body.code === 'string') {
      return {
        status: error.status,
        code: body.code,
        message: body.message ?? 'Something went wrong',
        fieldErrors: body.fieldErrors ?? [],
      };
    }
    if (error.status === 0) {
      return {
        status: 0,
        code: 'NETWORK',
        message: 'The server cannot be reached. Check your connection and try again.',
        fieldErrors: [],
      };
    }
    return {
      status: error.status,
      code: 'UNKNOWN',
      message: 'Something went wrong',
      fieldErrors: [],
    };
  }
  return { status: 0, code: 'UNKNOWN', message: 'Something went wrong', fieldErrors: [] };
}
