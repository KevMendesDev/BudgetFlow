import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { PageResponse } from '../models/pagination.models';
import { TransacaoRequest, TransacaoResponse } from '../models/transacao.models';

@Injectable({ providedIn: 'root' })
export class TransacoesApiService {
  private readonly http = inject(HttpClient);

  listByPeriodo(periodoId: number, dataInicio: string, dataFim: string): Observable<PageResponse<TransacaoResponse>> {
    const params = new HttpParams({
      fromObject: {
        periodoId: String(periodoId),
        dataInicio,
        dataFim,
        page: '0',
        size: '2000',
        sort: 'data,desc',
      },
    });

    return this.http.get<PageResponse<TransacaoResponse>>(`${API_BASE_URL}/api/transacoes`, { params });
  }

  create(payload: TransacaoRequest): Observable<TransacaoResponse> {
    return this.http.post<TransacaoResponse>(`${API_BASE_URL}/api/transacoes`, payload);
  }

  update(id: number, payload: TransacaoRequest): Observable<TransacaoResponse> {
    return this.http.put<TransacaoResponse>(`${API_BASE_URL}/api/transacoes/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/transacoes/${id}`);
  }
}
