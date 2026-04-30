import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, Observable, shareReplay, switchMap, throwError } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { RawHttpService } from '../services/raw-http.service';
import { SessionService } from '../services/session.service';

const AUTH_ROUTES_WITHOUT_REFRESH = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];
const UNAUTHORIZED_STATUS = new Set([401, 403]);

let refreshInFlight$: Observable<void> | null = null;

export const authRefreshInterceptor: HttpInterceptorFn = (req, next) => {
  const rawHttp = inject(RawHttpService);
  const session = inject(SessionService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isUnauthorized = UNAUTHORIZED_STATUS.has(error.status);
      const shouldSkipRefresh = AUTH_ROUTES_WITHOUT_REFRESH.some((route) => req.url.includes(route));

      if (!isUnauthorized || shouldSkipRefresh) {
        return throwError(() => error);
      }

      if (!refreshInFlight$) {
        refreshInFlight$ = rawHttp.http
          .post<void>(`${API_BASE_URL}/api/auth/refresh`, {}, { withCredentials: true })
          .pipe(
            shareReplay(1),
            finalize(() => {
              refreshInFlight$ = null;
            })
          );
      }

      return refreshInFlight$.pipe(
        switchMap(() => next(req)),
        catchError((refreshError) => {
          session.clearSession();

          const isAuthScreen = router.url.startsWith('/login') || router.url.startsWith('/cadastro');
          if (!isAuthScreen) {
            router.navigate(['/login']);
          }

          return throwError(() => refreshError);
        })
      );
    })
  );
};
