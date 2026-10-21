import type { Problem } from './generated';

/**
 * A failed API call. Carries the HTTP status (always) and the RFC 9457 problem
 * details (when the server sent a body). Structurally it is also a `Problem`, so it
 * matches the error types in the generated SDK.
 */
export class ApiError extends Error implements Problem {
  readonly status: number;
  readonly type?: string;
  readonly title?: string;
  readonly detail?: string;
  readonly instance?: string;

  constructor(status: number, problem?: Problem) {
    super(problem?.detail ?? problem?.title ?? `Request failed with status ${String(status)}`);
    this.name = 'ApiError';
    this.status = status;
    this.type = problem?.type;
    this.title = problem?.title;
    this.detail = problem?.detail;
    this.instance = problem?.instance;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

function asProblem(body: unknown): Problem | undefined {
  return typeof body === 'object' && body !== null ? body : undefined;
}

/**
 * Turns whatever the generated client throws for an HTTP error (a parsed JSON body,
 * raw text, or `{}` for an empty body) into an `ApiError`. Network failures have no
 * response and pass through untouched.
 */
export function toApiError(error: unknown, response: Response | undefined): unknown {
  if (!response || response.ok) {
    return error;
  }
  return new ApiError(response.status, asProblem(error));
}
