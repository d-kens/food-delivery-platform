import { HttpErrorResponse } from '@angular/common/http';

export interface ProblemDetail {
  status?: number;
  title?: string;
  detail?: string;
  errors?: Record<string, string[]>;
}

export function problemOf(error: unknown): ProblemDetail {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    return error.error as ProblemDetail;
  }
  return {};
}

export function isNetworkError(error: unknown): boolean {
  return error instanceof HttpErrorResponse && (error.status === 0 || error.status === 504);
}

export function statusOf(error: unknown): number | null {
  return error instanceof HttpErrorResponse ? error.status : null;
}

export const NETWORK_ERROR_MESSAGE =
  "Can't reach Keroma right now. Check your connection and try again.";
export const SERVER_ERROR_MESSAGE = 'Something went wrong on our side. Please try again.';

export function genericErrorMessage(error: unknown): string {
  return isNetworkError(error) ? NETWORK_ERROR_MESSAGE : SERVER_ERROR_MESSAGE;
}
