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

export const FREQUENCIA_LABELS: Record<Frequencia, string> = {
  DIARIO: 'Diário',
  SEMANAL: 'Semanal',
  MENSAL: 'Mensal',
  ANUAL: 'Anual',
};

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
}

export type TransacaoRecorrentePageResponse = PageResponse<TransacaoRecorrenteResponse>;
