import { ApiValidationError } from '../../core/models/auth.models';

export function mapApiError(err: unknown): string {
  const fallback = 'Falha na requisição. Tente de novo.';

  if (!err || typeof err !== 'object') {
    return fallback;
  }

  const maybeHttpError = err as { error?: ApiValidationError };

  if (maybeHttpError.error?.error) {
    return maybeHttpError.error.error;
  }

  const firstValidationError = maybeHttpError.error?.errors
    ? Object.values(maybeHttpError.error.errors)[0]
    : null;

  return firstValidationError ?? fallback;
}
