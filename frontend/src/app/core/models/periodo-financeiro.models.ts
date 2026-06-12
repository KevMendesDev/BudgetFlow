import { PageResponse } from './pagination.models';

export interface PeriodoFinanceiroRequest {
  mes: number;
  ano: number;
}

export interface PeriodoFinanceiroResponse {
  id: number;
  userId: number;
  mes: number;
  ano: number;
  dataInicio: string;
  dataFim: string;
  createdAt: string;
  updatedAt: string;
  possuiRelacionamentos: boolean;
}

export type PeriodoFinanceiro = PeriodoFinanceiroResponse;

export type PeriodoFinanceiroPageResponse = PageResponse<PeriodoFinanceiroResponse>;
