import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { map } from 'rxjs/operators';

import { SessionService } from '../services/session.service';

export const guestGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.isAuthenticated()) {
    return router.createUrlTree(['/dashboard']);
  }

  return session.bootstrapSession().pipe(
    map((ok) => (ok ? router.createUrlTree(['/dashboard']) : true))
  );
};
