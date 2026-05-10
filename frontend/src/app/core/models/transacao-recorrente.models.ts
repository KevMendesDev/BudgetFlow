import { PageResponse } from './pagination.models';
import { ClassificacaoCategoria } from './categoria.models';
import { TipoMovimentacao, TipoPagamento } from './transacao.models';

export type { TipoMovimentacao, TipoPagamento };

export type Frequencia = 'DIARIO' | 'SEMANAL' | 'MENSAL' | 'ANUAL';

export const FREQUENCIAS: Array<{ value: Frequencia; label: string }> = [
  { value: 'DIARIO', label: 'Diário' },
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
  valorParcela: number;
  tipoMovimentacao: TipoMovimentacao;
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
  classificacaoCategoria: ClassificacaoCategoria;
  descricao: string;
  valorParcela: number;
  valorTotal: number;
  tipoMovimentacao: TipoMovimentacao;
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
