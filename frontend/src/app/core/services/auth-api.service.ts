import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { CurrentUser, LoginRequest, RegisterRequest } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  register(payload: RegisterRequest): Observable<CurrentUser> {
    return this.http.post<CurrentUser>(`${API_BASE_URL}/api/auth/register`, payload);
  }

  login(payload: LoginRequest): Observable<CurrentUser> {
    return this.http.post<CurrentUser>(`${API_BASE_URL}/api/auth/login`, payload);
  }

  me(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${API_BASE_URL}/api/auth/me`);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/auth/logout`, {});
  }
}
