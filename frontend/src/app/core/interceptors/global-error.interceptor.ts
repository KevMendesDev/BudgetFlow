import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { ToastService } from '../services/toast.service';

const AUTH_ENDPOINTS_WITH_LOCAL_HANDLING = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];

export const globalErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const skipToastByRoute = AUTH_ENDPOINTS_WITH_LOCAL_HANDLING.some((route) => req.url.includes(route));
      const skipToastByRefreshTokenError = isRefreshTokenError(error);

      if (!skipToastByRoute && !skipToastByRefreshTokenError) {
        toastService.show(mapHttpErrorToMessage(error));
      }

      return throwError(() => error);
    })
  );
};

function mapHttpErrorToMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Sem conexão com o servidor.';
  }

  const payload = error.error as { error?: string; errors?: Record<string, string> } | null;

  if (payload?.error) {
    return payload.error;
  }

  const firstFieldError = payload?.errors ? Object.values(payload.errors)[0] : null;

  if (firstFieldError) {
    return firstFieldError;
  }

  if (error.status >= 500) {
    return 'Erro interno no servidor.';
  }

  if (error.status === 401) {
    return 'Sessão expirada. Faça login novamente.';
  }

  return 'Não foi possível concluir a requisição.';
}

function isRefreshTokenError(error: HttpErrorResponse): boolean {
  const payload = error.error as { error?: string } | null;
  const message = payload?.error?.toLowerCase();

  return !!message && message.includes('refresh token');
}
