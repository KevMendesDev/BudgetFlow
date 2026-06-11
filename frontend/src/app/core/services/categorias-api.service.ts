import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import {
  CategoriaPageResponse,
  CategoriaRequest,
  CategoriaResponse,
  ClassificacaoCategoria,
} from '../models/categoria.models';
import { NaturezaFinanceira } from '../models/natureza-financeira.models';
import { PageSize } from '../models/pagination.models';

@Injectable({ providedIn: 'root' })
export class CategoriasApiService {
  private readonly http = inject(HttpClient);

  listAll(filters?: {
    page?: number;
    size?: number;
    q?: string;
    classificacao?: ClassificacaoCategoria | '';
    tipoCategoria?: NaturezaFinanceira | '';
  }): Observable<CategoriaPageResponse> {
    let params = new HttpParams({
      fromObject: {
        page: String(filters?.page ?? 0),
        size: String(filters?.size ?? PageSize.LARGE),
        sort: 'nome,asc',
      },
    });

    const q = filters?.q?.trim();
    if (q) {
      params = params.set('q', q);
    }

    if (filters?.classificacao) {
      params = params.set('classificacao', filters.classificacao);
    }

    if (filters?.tipoCategoria) {
      params = params.set('tipoCategoria', filters.tipoCategoria);
    }

    return this.http.get<CategoriaPageResponse>(`${API_BASE_URL}/api/categorias`, { params });
  }

  create(payload: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(`${API_BASE_URL}/api/categorias`, payload);
  }

  update(id: number, payload: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.put<CategoriaResponse>(`${API_BASE_URL}/api/categorias/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/categorias/${id}`);
  }
}
