import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';

import { CsrfTokenService } from '../services/csrf-token.service';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (SAFE_METHODS.has(req.method)) {
    return next(req);
  }

  return inject(CsrfTokenService)
    .getToken()
    .pipe(
      switchMap(({ headerName, token }) =>
        next(req.clone({ setHeaders: { [headerName]: token } }))
      )
    );
};
