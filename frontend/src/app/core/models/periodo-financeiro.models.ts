import { PageResponse } from './transacao.models';

export interface PeriodoFinanceiroRequest {
  dataInicio: string;
  dataFim: string;
}

export interface PeriodoFinanceiroResponse {
  id: number;
  userId: number;
  dataInicio: string;
  dataFim: string;
  createdAt: string;
  updatedAt: string;
}

export type PeriodoFinanceiroPageResponse = PageResponse<PeriodoFinanceiroResponse>;
