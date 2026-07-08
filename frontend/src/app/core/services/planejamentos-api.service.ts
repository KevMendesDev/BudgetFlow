import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import {
  PlanejamentoRequest,
  PlanejamentoResponse,
  SincronizacaoPlanejamentosResponse,
} from '../models/planejamento.models';

@Injectable({ providedIn: 'root' })
export class PlanejamentosApiService {
  private readonly http = inject(HttpClient);

  listByPeriodo(periodoId: number): Observable<PlanejamentoResponse[]> {
    const params = new HttpParams().set('periodoId', String(periodoId));
    return this.http.get<PlanejamentoResponse[]>(`${API_BASE_URL}/api/planejamentos`, { params });
  }

  create(payload: PlanejamentoRequest): Observable<PlanejamentoResponse> {
    return this.http.post<PlanejamentoResponse>(`${API_BASE_URL}/api/planejamentos`, payload);
  }

  update(id: number, payload: PlanejamentoRequest): Observable<PlanejamentoResponse> {
    return this.http.put<PlanejamentoResponse>(`${API_BASE_URL}/api/planejamentos/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/planejamentos/${id}`);
  }

  sincronizarRecorrentes(periodoId: number): Observable<SincronizacaoPlanejamentosResponse> {
    const params = new HttpParams().set('periodoId', String(periodoId));
    return this.http.post<SincronizacaoPlanejamentosResponse>(
      `${API_BASE_URL}/api/planejamentos/sincronizar-recorrentes`,
      null,
      { params }
    );
  }
}
