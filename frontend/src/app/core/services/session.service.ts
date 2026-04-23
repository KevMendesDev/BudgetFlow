import { inject, Injectable, signal } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { catchError, finalize, map, shareReplay } from 'rxjs/operators';

import { CurrentUser } from '../models/auth.models';
import { AuthApiService } from './auth-api.service';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly authApi = inject(AuthApiService);
  private bootstrapInFlight$: Observable<boolean> | null = null;
  private hasBootstrapped = false;

  readonly user = signal<CurrentUser | null>(null);
  readonly loading = signal(false);

  readonly isAuthenticated = () => this.user() !== null;

  bootstrapSession(force = false): Observable<boolean> {
    if (this.hasBootstrapped && !force) {
      return of(this.isAuthenticated());
    }

    if (this.bootstrapInFlight$) {
      return this.bootstrapInFlight$;
    }

    this.loading.set(true);

    this.bootstrapInFlight$ = this.authApi.me().pipe(
      tap((currentUser) => {
        this.user.set(currentUser);
        this.hasBootstrapped = true;
      }),
      map(() => true),
      catchError(() => {
        this.user.set(null);
        this.hasBootstrapped = true;
        return of(false);
      }),
      finalize(() => {
        this.loading.set(false);
        this.bootstrapInFlight$ = null;
      }),
      shareReplay(1)
    );

    return this.bootstrapInFlight$;
  }

  setUser(currentUser: CurrentUser): void {
    this.user.set(currentUser);
    this.hasBootstrapped = true;
  }

  clearSession(): void {
    this.user.set(null);
    this.hasBootstrapped = true;
  }
}
