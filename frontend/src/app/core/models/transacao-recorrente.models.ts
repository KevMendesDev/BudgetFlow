import { PageResponse } from './pagination.models';
import { ClassificacaoCategoria } from './categoria.models';
import { NaturezaFinanceira } from './natureza-financeira.models';
import { TipoPagamento } from './transacao.models';

export { NaturezaFinanceira };
export type { TipoPagamento };

export type Frequencia = 'SEMANAL' | 'MENSAL' | 'ANUAL';

export const FREQUENCIAS: Array<{ value: Frequencia; label: string }> = [
  { value: 'SEMANAL', label: 'Semanal' },
  { value: 'MENSAL', label: 'Mensal' },
  { value: 'ANUAL', label: 'Anual' },
];

export const FREQUENCIA_LABELS = Object.fromEntries(
  FREQUENCIAS.map(({ value, label }) => [value, label])
) as Record<Frequencia, string>;

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
  createdAt: string;
  updatedAt: string;
  possuiRelacionamentos: boolean;
}

export type TransacaoRecorrentePageResponse = PageResponse<TransacaoRecorrenteResponse>;
