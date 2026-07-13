import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { NaturezaFinanceira } from '../models/natureza-financeira.models';
import { PageResponse, PageSize } from '../models/pagination.models';
import {
  SincronizacaoRecorrentesResponse,
  StatusTransacao,
  TipoPagamento,
  TransacaoRequest,
  TransacaoResponse,
} from '../models/transacao.models';

export interface TransacaoListParams {
  periodoId: number;
  dataInicio: string;
  dataFim: string;
  page?: number;
  size?: number;
  query?: string;
  classificacaoCategoria?: string;
  tipoMovimentacao?: NaturezaFinanceira;
  tipoPagamento?: TipoPagamento;
  status?: StatusTransacao;
  recorrente?: boolean;
}

@Injectable({ providedIn: 'root' })
export class TransacoesApiService {
  private readonly http = inject(HttpClient);

  listByPeriodo(periodoId: number, dataInicio: string, dataFim: string): Observable<PageResponse<TransacaoResponse>> {
    let params = new HttpParams({
      fromObject: {
        periodoId: String(periodoId),
        dataInicio,
        dataFim,
        page: '0',
        size: String(PageSize.BULK),
      },
    });
    params = params.append('sort', 'data,desc').append('sort', 'createdAt,desc');

    return this.http.get<PageResponse<TransacaoResponse>>(`${API_BASE_URL}/api/transacoes`, { params });
  }

  listByPeriodoFiltered(params: TransacaoListParams): Observable<PageResponse<TransacaoResponse>> {
    let httpParams = new HttpParams({
      fromObject: {
        periodoId: String(params.periodoId),
        dataInicio: params.dataInicio,
        dataFim: params.dataFim,
        page: String(params.page ?? 0),
        size: String(params.size ?? PageSize.DEFAULT),
      },
    });
    httpParams = httpParams.append('sort', 'data,desc').append('sort', 'createdAt,desc');

    if (params.query) {
      httpParams = httpParams.set('query', params.query);
    }
    if (params.classificacaoCategoria) {
      httpParams = httpParams.set('classificacaoCategoria', params.classificacaoCategoria);
    }
    if (params.tipoMovimentacao) {
      httpParams = httpParams.set('tipoMovimentacao', params.tipoMovimentacao);
    }
    if (params.tipoPagamento) {
      httpParams = httpParams.set('tipoPagamento', params.tipoPagamento);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.recorrente !== undefined) {
      httpParams = httpParams.set('recorrente', String(params.recorrente));
    }

    return this.http.get<PageResponse<TransacaoResponse>>(`${API_BASE_URL}/api/transacoes`, {
      params: httpParams,
    });
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

  sincronizarRecorrentes(periodoId: number): Observable<SincronizacaoRecorrentesResponse> {
    const params = new HttpParams().set('periodoId', String(periodoId));
    return this.http.post<SincronizacaoRecorrentesResponse>(
      `${API_BASE_URL}/api/transacoes/sincronizar-recorrentes`,
      null,
      { params }
    );
  }
}
