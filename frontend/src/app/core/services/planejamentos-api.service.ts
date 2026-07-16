import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { ClassificacaoCategoria } from '../models/categoria.models';
import { NaturezaFinanceira } from '../models/natureza-financeira.models';
import { PageSize } from '../models/pagination.models';
import {
  PlanejamentoPageResponse,
  PlanejamentoRequest,
  PlanejamentoResponse,
  SincronizacaoPlanejamentosResponse,
} from '../models/planejamento.models';

export interface PlanejamentoListParams {
  periodoId: number;
  page?: number;
  size?: number;
  query?: string;
  tipoMovimentacao?: NaturezaFinanceira | '';
  classificacaoCategoria?: ClassificacaoCategoria | '';
}

@Injectable({ providedIn: 'root' })
export class PlanejamentosApiService {
  private readonly http = inject(HttpClient);

  listByPeriodo(periodoId: number, size = PageSize.BULK): Observable<PlanejamentoPageResponse> {
    const params = new HttpParams({
      fromObject: {
        periodoId: String(periodoId),
        page: '0',
        size: String(size),
        sort: 'createdAt,desc',
      },
    });
    return this.http.get<PlanejamentoPageResponse>(`${API_BASE_URL}/api/planejamentos`, { params });
  }

  listFiltered(filters: PlanejamentoListParams): Observable<PlanejamentoPageResponse> {
    let params = new HttpParams({
      fromObject: {
        periodoId: String(filters.periodoId),
        page: String(filters.page ?? 0),
        size: String(filters.size ?? PageSize.DEFAULT),
        sort: 'createdAt,desc',
      },
    });

    const query = filters.query?.trim();
    if (query) {
      params = params.set('query', query);
    }
    if (filters.tipoMovimentacao) {
      params = params.set('tipoMovimentacao', filters.tipoMovimentacao);
    }
    if (filters.classificacaoCategoria) {
      params = params.set('classificacaoCategoria', filters.classificacaoCategoria);
    }

    return this.http.get<PlanejamentoPageResponse>(`${API_BASE_URL}/api/planejamentos`, { params });
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

  deleteAllByPeriodo(periodoId: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/planejamentos/periodo/${periodoId}`);
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
