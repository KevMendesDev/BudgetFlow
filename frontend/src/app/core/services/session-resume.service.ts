import { inject, Injectable } from '@angular/core';

import { CsrfTokenService } from './csrf-token.service';
import { SessionService } from './session.service';

/**
 * Invalida o cache CSRF ao retomar o app (PWA / aba em background),
 * evitando 403 por token desalinhado do cookie após idle longo.
 */
@Injectable({ providedIn: 'root' })
export class SessionResumeService {
  private readonly csrfTokenService = inject(CsrfTokenService);
  private readonly session = inject(SessionService);
  private started = false;

  start(): void {
    if (this.started || typeof document === 'undefined') {
      return;
    }

    this.started = true;

    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        this.onResume();
      }
    });

    window.addEventListener('pageshow', () => {
      this.onResume();
    });
  }

  onResume(): void {
    this.csrfTokenService.invalidate();

    if (this.session.isAuthenticated()) {
      this.session.bootstrapSession(true).subscribe();
    }
  }
}
