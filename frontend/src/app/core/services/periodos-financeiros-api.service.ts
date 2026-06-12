import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import {
  PeriodoFinanceiroPageResponse,
  PeriodoFinanceiroRequest,
  PeriodoFinanceiroResponse,
} from '../models/periodo-financeiro.models';
import { PageSize } from '../models/pagination.models';

@Injectable({ providedIn: 'root' })
export class PeriodosFinanceirosApiService {
  private readonly http = inject(HttpClient);

  listAll(filters?: {
    page?: number;
    size?: number;
    q?: string;
    dataInicio?: string;
    dataFim?: string;
  }): Observable<PeriodoFinanceiroPageResponse> {
    let params = new HttpParams({
      fromObject: {
        page: String(filters?.page ?? 0),
        size: String(filters?.size ?? PageSize.LARGE),
        sort: 'dataInicio,desc',
      },
    });

    const q = filters?.q?.trim();
    if (q) {
      params = params.set('q', q);
    }

    if (filters?.dataInicio) {
      params = params.set('dataInicio', filters.dataInicio);
    }

    if (filters?.dataFim) {
      params = params.set('dataFim', filters.dataFim);
    }

    return this.http.get<PeriodoFinanceiroPageResponse>(`${API_BASE_URL}/api/periodos-financeiros`, { params });
  }

  create(payload: PeriodoFinanceiroRequest): Observable<PeriodoFinanceiroResponse> {
    return this.http.post<PeriodoFinanceiroResponse>(`${API_BASE_URL}/api/periodos-financeiros`, payload);
  }

  update(id: number, payload: PeriodoFinanceiroRequest): Observable<PeriodoFinanceiroResponse> {
    return this.http.put<PeriodoFinanceiroResponse>(`${API_BASE_URL}/api/periodos-financeiros/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/periodos-financeiros/${id}`);
  }
}
