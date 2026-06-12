import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { PageResponse, PageSize } from '../models/pagination.models';
import { PeriodoFinanceiro } from '../models/periodo-financeiro.models';

@Injectable({ providedIn: 'root' })
export class PeriodosApiService {
  private readonly http = inject(HttpClient);

  listAll(): Observable<PageResponse<PeriodoFinanceiro>> {
    const params = new HttpParams({
      fromObject: {
        page: '0',
        size: String(PageSize.LARGE),
        sort: 'dataInicio,desc',
      },
    });

    return this.http.get<PageResponse<PeriodoFinanceiro>>(`${API_BASE_URL}/api/periodos-financeiros`, {
      params,
    });
  }
}
