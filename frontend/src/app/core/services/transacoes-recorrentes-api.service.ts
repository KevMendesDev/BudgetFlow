import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { NaturezaFinanceira } from '../models/natureza-financeira.models';
import { PageSize } from '../models/pagination.models';
import {
  Frequencia,
  TransacaoRecorrentePageResponse,
  TransacaoRecorrenteRequest,
  TransacaoRecorrenteResponse,
} from '../models/transacao-recorrente.models';

@Injectable({ providedIn: 'root' })
export class TransacoesRecorrentesApiService {
  private readonly http = inject(HttpClient);

  listAll(filters?: {
    page?: number;
    size?: number;
    query?: string;
    frequencia?: Frequencia | '';
    tipoMovimentacao?: NaturezaFinanceira | '';
  }): Observable<TransacaoRecorrentePageResponse> {
    let params = new HttpParams({
      fromObject: {
        page: String(filters?.page ?? 0),
        size: String(filters?.size ?? PageSize.LARGE),
        sort: 'createdAt,desc',
      },
    });

    const query = filters?.query?.trim();
    if (query) {
      params = params.set('query', query);
    }

    if (filters?.frequencia) {
      params = params.set('frequencia', filters.frequencia);
    }

    if (filters?.tipoMovimentacao) {
      params = params.set('tipoMovimentacao', filters.tipoMovimentacao);
    }

    return this.http.get<TransacaoRecorrentePageResponse>(`${API_BASE_URL}/api/transacoes-recorrentes`, { params });
  }

  create(payload: TransacaoRecorrenteRequest): Observable<TransacaoRecorrenteResponse> {
    return this.http.post<TransacaoRecorrenteResponse>(`${API_BASE_URL}/api/transacoes-recorrentes`, payload);
  }

  update(id: number, payload: TransacaoRecorrenteRequest): Observable<TransacaoRecorrenteResponse> {
    return this.http.put<TransacaoRecorrenteResponse>(`${API_BASE_URL}/api/transacoes-recorrentes/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/transacoes-recorrentes/${id}`);
  }
}
