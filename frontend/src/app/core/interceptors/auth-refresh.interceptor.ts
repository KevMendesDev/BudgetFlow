import {
  HttpContextToken,
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, Observable, shareReplay, switchMap, throwError } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { CsrfTokenService } from '../services/csrf-token.service';
import { RawHttpService } from '../services/raw-http.service';
import { SessionService } from '../services/session.service';

const AUTH_ROUTES_WITHOUT_REFRESH = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
const UNAUTHORIZED_STATUS = 401;
const FORBIDDEN_STATUS = 403;
export const CSRF_RECOVERY_ATTEMPTED = new HttpContextToken<boolean>(() => false);

let refreshInFlight$: Observable<void> | null = null;

export function resetAuthRefreshStateForTests(): void {
  refreshInFlight$ = null;
}

export const authRefreshInterceptor: HttpInterceptorFn = (req, next) => {
  const rawHttp = inject(RawHttpService);
  const csrfTokenService = inject(CsrfTokenService);
  const session = inject(SessionService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const shouldSkipAuthRoutes = AUTH_ROUTES_WITHOUT_REFRESH.some((route) => req.url.includes(route));

      if (
        error.status === FORBIDDEN_STATUS &&
        !shouldSkipAuthRoutes &&
        MUTATING_METHODS.has(req.method)
      ) {
        if (req.context.get(CSRF_RECOVERY_ATTEMPTED)) {
          return throwError(() => error);
        }

        return retryWithFreshCsrf(req, next, csrfTokenService).pipe(
          catchError((retryError: HttpErrorResponse) => {
            if (retryError.status === UNAUTHORIZED_STATUS && !shouldSkipAuthRoutes) {
              return refreshAndRetry(req, next, rawHttp, csrfTokenService, session, router);
            }

            return throwError(() => retryError);
          })
        );
      }

      if (error.status !== UNAUTHORIZED_STATUS || shouldSkipAuthRoutes) {
        return throwError(() => error);
      }

      return refreshAndRetry(req, next, rawHttp, csrfTokenService, session, router);
    })
  );
};

function retryWithFreshCsrf(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  csrfTokenService: CsrfTokenService
): Observable<HttpEvent<unknown>> {
  csrfTokenService.invalidate();

  return csrfTokenService.getToken().pipe(
    switchMap(({ headerName, token }) =>
      next(
        req.clone({
          headers: req.headers.set(headerName, token),
          context: req.context.set(CSRF_RECOVERY_ATTEMPTED, true),
        })
      )
    )
  );
}

function refreshAndRetry(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  rawHttp: RawHttpService,
  csrfTokenService: CsrfTokenService,
  session: SessionService,
  router: Router
): Observable<HttpEvent<unknown>> {
  if (!refreshInFlight$) {
    csrfTokenService.invalidate();
    refreshInFlight$ = csrfTokenService.getToken().pipe(
      switchMap(({ headerName, token }) =>
        rawHttp.http.post<void>(`${API_BASE_URL}/api/auth/refresh`, {}, {
          withCredentials: true,
          headers: { [headerName]: token },
        })
      ),
      shareReplay(1),
      finalize(() => {
        refreshInFlight$ = null;
      })
    );
  }

  return refreshInFlight$.pipe(
    switchMap(() => {
      csrfTokenService.invalidate();

      if (!MUTATING_METHODS.has(req.method)) {
        return next(req);
      }

      return csrfTokenService.getToken().pipe(
        switchMap(({ headerName, token }) =>
          next(
            req.clone({
              headers: req.headers.set(headerName, token),
            })
          )
        )
      );
    }),
    catchError((refreshError) => {
      session.clearSession();

      const isAuthScreen = router.url.startsWith('/login') || router.url.startsWith('/cadastro');
      if (!isAuthScreen) {
        router.navigate(['/login']);
      }

      return throwError(() => refreshError);
    })
  );
}
