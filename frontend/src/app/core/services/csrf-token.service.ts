import { HttpBackend, HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { finalize, Observable, of, shareReplay, tap } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';

interface CsrfResponse {
  token: string;
  headerName: string;
}

@Injectable({ providedIn: 'root' })
export class CsrfTokenService {
  private readonly http = new HttpClient(inject(HttpBackend));
  private tokenRequest$: Observable<CsrfResponse> | null = null;
  private token: CsrfResponse | null = null;

  getToken(): Observable<CsrfResponse> {
    if (this.token) {
      return of(this.token);
    }

    if (!this.tokenRequest$) {
      this.tokenRequest$ = this.http
        .get<CsrfResponse>(`${API_BASE_URL}/api/auth/csrf`, { withCredentials: true })
        .pipe(
          tap((token) => {
            this.token = token;
          }),
          shareReplay(1),
          finalize(() => {
            this.tokenRequest$ = null;
          })
        );
    }

    return this.tokenRequest$;
  }

  invalidate(): void {
    this.token = null;
    this.tokenRequest$ = null;
  }
}
