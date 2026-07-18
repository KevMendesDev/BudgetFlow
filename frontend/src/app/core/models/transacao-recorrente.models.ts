import { PageResponse } from './pagination.models';
import { ClassificacaoCategoria } from './categoria.models';
import { NaturezaFinanceira } from './natureza-financeira.models';
import { TipoPagamento } from './transacao.models';

export { NaturezaFinanceira };
export type { TipoPagamento };

export type Frequencia = 'SEMANAL' | 'MENSAL' | 'ANUAL';
export type StatusRecorrencia = 'ATIVA' | 'INATIVA' | 'FINALIZADA';

export const FREQUENCIAS: Array<{ value: Frequencia; label: string }> = [
  { value: 'SEMANAL', label: 'Semanal' },
  { value: 'MENSAL', label: 'Mensal' },
  { value: 'ANUAL', label: 'Anual' },
];

export const STATUS_RECORRENCIA: Array<{ value: StatusRecorrencia; label: string }> = [
  { value: 'ATIVA', label: 'Ativa' },
  { value: 'INATIVA', label: 'Inativa' },
  { value: 'FINALIZADA', label: 'Finalizada' },
];

export const STATUS_RECORRENCIA_EDITAVEIS: Array<{ value: StatusRecorrencia; label: string }> = [
  { value: 'ATIVA', label: 'Ativa' },
  { value: 'INATIVA', label: 'Inativa' },
];

export const FREQUENCIA_LABELS = Object.fromEntries(
  FREQUENCIAS.map(({ value, label }) => [value, label])
) as Record<Frequencia, string>;

export const STATUS_RECORRENCIA_LABELS = Object.fromEntries(
  STATUS_RECORRENCIA.map(({ value, label }) => [value, label])
) as Record<StatusRecorrencia, string>;

export interface TransacaoRecorrenteRequest {
  categoriaId: number;
  descricao: string;
  valorParcela: number | null;
  tipoMovimentacao: NaturezaFinanceira;
  tipoPagamento: TipoPagamento;
  frequencia: Frequencia;
  dataInicio: string;
  dataFim: string | null;
  totalParcelas: number | null;
  status: StatusRecorrencia;
}

export interface TransacaoRecorrenteResponse {
  id: number;
  userId: number;
  categoriaId: number;
  categoriaNome: string;
  classificacaoCategoria: ClassificacaoCategoria | null;
  descricao: string;
  valorParcela: number | null;
  valorTotal: number | null;
  tipoMovimentacao: NaturezaFinanceira;
  tipoPagamento: TipoPagamento;
  frequencia: Frequencia;
  dataInicio: string;
  dataFim: string | null;
  totalParcelas: number | null;
  status: StatusRecorrencia;
  createdAt: string;
  updatedAt: string;
  possuiRelacionamentos: boolean;
}

export type TransacaoRecorrentePageResponse = PageResponse<TransacaoRecorrenteResponse>;
